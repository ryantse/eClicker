package edu.ucsb.cs.cs184.elicker.eclicker;
import com.neovisionaries.ws.client.*;

import org.json.JSONException;

import java.net.URI;
import java.util.*;

public class ConnectionManager {
    private static ConnectionManager connectionManager = null;
    final static String serverHost = "10.0.0.96:3000";
    private final WebSocket webSocket;
    private boolean isConnected = false;
    private final HashMap<MessageType, ArrayList<MessageListenerContainer>> messageListeners;

    private ConnectionManager() {
        try {
            this.webSocket = new WebSocketFactory().createSocket(new URI("ws://" + serverHost + "/mobile/session"), 5000);
            this.webSocket.connectAsynchronously();
            this.webSocket.addListener(new WebSocketListener(this));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        messageListeners = new HashMap<>();
        for (MessageType messageType : MessageType.values()) {
            messageListeners.put(messageType, new ArrayList<MessageListenerContainer>());
        }
    }

    public static ConnectionManager getInstance() {
        if(connectionManager == null) {
            connectionManager = new ConnectionManager();
        }

        return connectionManager;
    }

    public void onConnected() {
        this.isConnected = true;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public void onTextMessage(String strJson) throws JSONException {
        MessageReceived messageReceived = new MessageReceived(strJson);
        Iterator<MessageListenerContainer> messageListenerContainerIterator = messageListeners.get(messageReceived.getMessageType()).iterator();
        while(messageListenerContainerIterator.hasNext()) {
            MessageListenerContainer messageListenerContainer = messageListenerContainerIterator.next();

            messageListenerContainer.messageListener.handleMessage(messageReceived);

            if(messageListenerContainer.listenerType == MessageListenerType.ONCE) {
                messageListenerContainerIterator.remove();
            }
        }
    }

    public void onMessage(MessageType messageType, MessageListener listener) {
        messageListeners.get(messageType).add(new MessageListenerContainer(MessageListenerType.ALWAYS, listener));
    }

    public void onMessageOnce(MessageType messageType, MessageListener listener) {
        messageListeners.get(messageType).add(new MessageListenerContainer(MessageListenerType.ONCE, listener));
    }

    public void sendMessage(String message) {
        System.out.println(message);
        this.webSocket.sendText(message);
    }

    public interface MessageListener {
        void handleMessage(MessageReceived messageReceived);
    }

    private class MessageListenerContainer {
        MessageListenerType listenerType;
        MessageListener messageListener;


        MessageListenerContainer(MessageListenerType messageListenerType, MessageListener messageListener) {
            this.listenerType = messageListenerType;
            this.messageListener = messageListener;
        }
    }

    private class WebSocketListener extends WebSocketAdapter {
        private ConnectionManager connectionManager;

        WebSocketListener(ConnectionManager connectionManager) {
            super();
            this.connectionManager = connectionManager;
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            connectionManager.onTextMessage(text);
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            connectionManager.onConnected();
        }
    }

    enum MessageListenerType {ONCE, ALWAYS};
}

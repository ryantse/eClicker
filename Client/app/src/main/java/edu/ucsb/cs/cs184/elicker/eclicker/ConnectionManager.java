package edu.ucsb.cs.cs184.elicker.eclicker;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConnectionManager {
    final static String serverHost = "10.0.0.52:3000";
    private static ConnectionManager connectionManager = null;
    private final WebSocket webSocket;
    private final HashMap<MessageType, ArrayList<MessageListenerContainer>> messageListeners;
    private boolean isConnected = false;

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
        if (connectionManager == null) {
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

    public void onTextMessage(String text) {
        MessageType messageType;
        String messageTypeString;
        JSONObject messageData;

        try {
            JSONObject jsonObject = new JSONObject(text);
            messageTypeString = jsonObject.getString("messageType");
            messageData = jsonObject.getJSONObject("messageData");
            messageType = MessageType.valueOf(messageTypeString);
        } catch (IllegalArgumentException | NullPointerException | JSONException exception) {
            System.err.printf("Error processing message %s.\n", text);
            return;
        }

        Iterator<MessageListenerContainer> messageListenerContainerIterator = messageListeners.get(messageType).iterator();

        while (messageListenerContainerIterator.hasNext()) {
            MessageListenerContainer messageListenerContainer = messageListenerContainerIterator.next();
            messageListenerContainer.messageListener.handleMessage(messageData);

            if (messageListenerContainer.listenerType == MessageListenerType.ONCE) {
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

    public void sendMessage(MessageType messageType, JSONObject messageData) {
        try {
            JSONObject message = new JSONObject();
            message.put("messageType", messageType.toString());
            message.put("messageData", messageData);
            this.webSocket.sendText(message.toString());
        } catch (JSONException exception) {
            System.err.printf("Failed to send message %s.\n", messageType.toString());
        }
    }

    enum MessageListenerType {ONCE, ALWAYS}

    enum MessageType {DEVICE_JOIN, SESSION_JOIN, QUESTION_BEGIN}

    public interface MessageListener {
        void handleMessage(JSONObject messageData);
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
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            super.onConnectError(websocket, exception);
            System.err.println(exception);
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
}

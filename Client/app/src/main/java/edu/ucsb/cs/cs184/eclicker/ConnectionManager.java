package edu.ucsb.cs.cs184.eclicker;

import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ConnectionManager implements WebSocketListener {
    final static String serverHost = "eclickerapp.com";
    private static final ArrayList<MessageListenerContainer> networkFailureListeners = new ArrayList<>();
    private static ConnectionManager connectionManager = null;
    private final WebSocket webSocket;
    private final EnumMap<MessageType, ArrayList<MessageListenerContainer>> messageListeners;
    private boolean isConnected = false;

    private ConnectionManager() {
        messageListeners = new EnumMap<>(MessageType.class);
        for (MessageType messageType : MessageType.values()) {
            messageListeners.put(messageType, new ArrayList<MessageListenerContainer>());
        }

        try {
            this.webSocket = new WebSocketFactory().createSocket(new URI("wss://" + serverHost + "/mobile/session"), 3000);
            this.webSocket.connectAsynchronously();
            this.webSocket.addListener(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static WeakReference<ConnectionManager> getInstance() {
        if (connectionManager == null) {
            synchronized (ConnectionManager.class) {
                if (connectionManager == null) {
                    connectionManager = new ConnectionManager();
                }
            }
        }

        /* We return a weak reference since this connection manager may be destroyed
         * when the network changes.
         */
        return new WeakReference<>(connectionManager);
    }

    public static MessageListenerReference onNetworkError(MessageListener listener) {
        MessageListenerContainer messageListenerContainer = new MessageListenerContainer(null, MessageListenerType.ALWAYS, listener);
        networkFailureListeners.add(messageListenerContainer);
        return messageListenerContainer;
    }

    public static MessageListenerReference onNetworkErrorOnce(MessageListener listener) {
        MessageListenerContainer messageListenerContainer = new MessageListenerContainer(null, MessageListenerType.ONCE, listener);
        networkFailureListeners.add(messageListenerContainer);
        return messageListenerContainer;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public MessageListenerContainer onMessage(MessageType messageType, MessageListener listener) {
        MessageListenerContainer messageListenerContainer = new MessageListenerContainer(messageType, MessageListenerType.ALWAYS, listener);
        messageListeners.get(messageType).add(messageListenerContainer);
        return messageListenerContainer;
    }

    public MessageListenerContainer onMessageOnce(MessageType messageType, MessageListener listener) {
        MessageListenerContainer messageListenerContainer = new MessageListenerContainer(messageType, MessageListenerType.ONCE, listener);
        messageListeners.get(messageType).add(messageListenerContainer);
        return messageListenerContainer;
    }

    public void removeMessageListener(MessageListenerReference messageListenerReference) {
        MessageListenerContainer messageListenerContainer;
        try {
            messageListenerContainer = (MessageListenerContainer) messageListenerReference;
        } catch (ClassCastException exception) {
            return;
        }

        if (messageListenerContainer.messageType != null) {
            messageListeners.get(messageListenerContainer.messageType).remove(messageListenerContainer);
        } else {
            networkFailureListeners.remove(messageListenerContainer);
        }
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

    @Override
    public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {

    }

    private void onConnected() {
        this.isConnected = true;
        this.webSocket.setPingInterval(5 * 1000);

        ArrayList<MessageListener> messageListeners = new ArrayList<>();

        // First we find all the message listeners that are interested in this event.
        Iterator<MessageListenerContainer> messageListenerContainerIterator = this.messageListeners.get(MessageType.GENERAL_NETWORK_CONNECTED).iterator();
        while (messageListenerContainerIterator.hasNext()) {
            MessageListenerContainer messageListenerContainer = messageListenerContainerIterator.next();
            messageListeners.add(messageListenerContainer.messageListener);

            /* In the event that the message listener calls the function again, we don't want to still have
             * a once type event listener to be called again.
             */
            if (messageListenerContainer.listenerType == MessageListenerType.ONCE) {
                messageListenerContainerIterator.remove();
            }
        }

        // Now we call the interested listeners.
        for (MessageListener messageListener : messageListeners) {
            messageListener.handleMessage(null);
        }
    }

    private void onConnectError() {
        connectionManager = null;

        // Since this connection manager is obsolete, let's remove any remaining references to listeners.
        this.messageListeners.clear();

        ArrayList<MessageListener> messageListeners = new ArrayList<>();

        Iterator<MessageListenerContainer> messageListenerContainerIterator = networkFailureListeners.iterator();
        while (messageListenerContainerIterator.hasNext()) {
            MessageListenerContainer messageListenerContainer = messageListenerContainerIterator.next();
            messageListeners.add(messageListenerContainer.messageListener);

            /* In the event that the message listener calls the function again, we don't want to still have
             * a once type event listener to be called again.
             */
            if (messageListenerContainer.listenerType == MessageListenerType.ONCE) {
                messageListenerContainerIterator.remove();
            }
        }

        // Now we call the interested listeners.
        for (MessageListener messageListener : messageListeners) {
            messageListener.handleMessage(null);
        }
    }

    private void onDisconnected() {
        connectionManager = null;

        // We are no longer connected.
        this.isConnected = false;

        ArrayList<MessageListener> messageListeners = new ArrayList<>();

        // First we find all the message listeners that are interested in this event.
        Iterator<MessageListenerContainer> messageListenerContainerIterator = this.messageListeners.get(MessageType.GENERAL_NETWORK_DISCONNECTED).iterator();
        while (messageListenerContainerIterator.hasNext()) {
            MessageListenerContainer messageListenerContainer = messageListenerContainerIterator.next();
            messageListeners.add(messageListenerContainer.messageListener);

            /* In the event that the message listener calls the function again, we don't want to still have
             * a once type event listener to be called again.
             */
            if (messageListenerContainer.listenerType == MessageListenerType.ONCE) {
                messageListenerContainerIterator.remove();
            }
        }

        // Since this connection manager is obsolete, let's remove any remaining references to listeners.
        this.messageListeners.clear();

        // Now we call the interested listeners.
        for (MessageListener messageListener : messageListeners) {
            messageListener.handleMessage(null);
        }
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        this.onConnected();
    }

    @Override
    public void onConnectError(WebSocket websocket, WebSocketException cause) throws Exception {
        this.onConnectError();
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        this.onDisconnected();
    }

    @Override
    public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onTextMessage(WebSocket websocket, String text) throws Exception {
        MessageType messageType;
        JSONObject messageData;
        ArrayList<MessageListener> messageListeners = new ArrayList<>();

        System.out.println(text);

        try {
            JSONObject jsonObject = new JSONObject(text);
            messageData = jsonObject.getJSONObject("messageData");
            messageType = MessageType.valueOf(jsonObject.getString("messageType"));
        } catch (IllegalArgumentException | NullPointerException | JSONException exception) {
            System.err.printf("Error processing message %s.\n", text);
            return;
        }

        // First we find all the message listeners that are interested in this event.
        Iterator<MessageListenerContainer> messageListenerContainerIterator = this.messageListeners.get(messageType).iterator();
        while (messageListenerContainerIterator.hasNext()) {
            MessageListenerContainer messageListenerContainer = messageListenerContainerIterator.next();

            /* In the event that the message listener calls the function again, we don't want to still have
             * a once type event listener to be called again.
             */
            if (messageListenerContainer.listenerType == MessageListenerType.ONCE) {
                messageListenerContainerIterator.remove();
            }

            messageListeners.add(messageListenerContainer.messageListener);
        }

        // Now we call the interested listeners.
        for (MessageListener messageListener : messageListeners) {
            messageListener.handleMessage(messageData);
        }
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {

    }

    @Override
    public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

    }

    @Override
    public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

    }

    @Override
    public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {

    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        System.err.printf("onError: %s\n", cause);
    }

    @Override
    public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {

    }

    @Override
    public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {

    }

    @Override
    public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {

    }

    @Override
    public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {

    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {

    }

    @Override
    public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {

    }

    @Override
    public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {

    }

    enum MessageListenerType {ONCE, ALWAYS}

    enum MessageType {GENERAL_NETWORK_DISCONNECTED, GENERAL_NETWORK_CONNECTED, DEVICE_JOIN, SESSION_JOIN, SESSION_TERMINATE, QUESTION_WAITING, QUESTION_BEGIN, QUESTION_END}

    public interface MessageListener {
        void handleMessage(JSONObject messageData);
    }

    private static class MessageListenerContainer extends MessageListenerReference {
        final MessageListenerType listenerType;
        final MessageListener messageListener;
        final MessageType messageType;

        MessageListenerContainer(MessageType messageType, MessageListenerType messageListenerType, MessageListener messageListener) {
            this.listenerType = messageListenerType;
            this.messageListener = messageListener;
            this.messageType = messageType;
        }
    }

    public static class MessageListenerReference {
    }
}

package edu.ucsb.cs.cs184.elicker.eclicker;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/*
    Handled message formats

    Device join message types.
    1.
    messageType: "DEVICE_JOIN",
    messageData: {
        status: "OK"
    }
    2.
    messageType: "DEVICE_JOIN",
    messageData: {
        status: "ERROR",
        statusExtended: {
            errorReason: "DEVICE_TOKEN_INVALID"
        }
    }
    Session join message types.
    3.
    messageType: "SESSION_JOIN",
    messageData: {
        status: "ERROR",
        statusExtended: {
            errorReason: "DEVICE_NOT_IDENTIFIED"
        }
    }
    4.
    messageType: "SESSION_JOIN",
    messageData: {
        status: "OK",
        statusExtended: {
            "authenticationType": "sessionKey",
            "sessionId": sessionId,
            "sessionToken": SessionTokenManager.generateSessionToken(sessionId, this.deviceId)
        }
    }
*/

public class MessageReceived {
    private MessageType messageType;
    private String status;
    private String errorMessage;

    // for device session join only
    private String authenticationType;
    private String sessionId;
    private String sessionToken;
    // for questions
    private String questionId;
    private String questionType;
    private String questionTitle;
    // question data
    private boolean scramble;
    private ArrayList<String> choices;


    public MessageReceived(String jsonString) throws JSONException {
        JSONObject obj = new JSONObject(jsonString);
        this.messageType = MessageType.valueOf(obj.getString("messageType"));
        JSONObject messageData = obj.getJSONObject("messageData");
        switch (messageType) {
            case DEVICE_JOIN:
                this.status = messageData.getString("status");
                switch (status) {
                    case "OK":
                        break;
                    case "ERROR":
                        JSONObject statusExtended = messageData.getJSONObject("statusExtended");
                        this.errorMessage = statusExtended.getString("errorReason");
                        break;
                    default:
                        System.err.println("Unknown status received: " + status + " with message type " + messageType);
                        break;
                }
                break;
            case SESSION_JOIN:
                this.status = messageData.getString("status");
                JSONObject statusExtended = messageData.getJSONObject("statusExtended");
                switch (status) {
                    case "OK":
                        this.authenticationType = statusExtended.getString("authenticationType");
                        this.sessionId = statusExtended.getString("sessionId");
                        this.sessionToken = statusExtended.getString("sessionToken");
                        break;
                    case "ERROR":
                        this.errorMessage = statusExtended.getString("errorReason");
                        break;
                    default:
                        System.err.println("Unknown status received: " + status + " with message type " + messageType);
                        break;
                }
                break;
            case QUESTION_BEGIN:
                this.questionId = messageData.getString("questionId");
                this.questionType = messageData.getString("questionType");
                this.questionTitle = messageData.getString("questionTitle");
                switch (questionType) {
                    case "MultipleChoice":
                    case "RankedChoice":
                        JSONObject questionData = messageData.getJSONObject("questionData");
                        this.scramble = questionData.getBoolean("scramble");
                        this.choices = new ArrayList<>();
                        JSONArray jArray = questionData.getJSONArray("choices");
                        if (jArray != null) {
                            for (int i=0;i<jArray.length();i++){
                                this.choices.add(jArray.getString(i));
                            }
                        }
                        break;
                    case "BooleanAnswer":
                    case "ShortAnswer":
                        break;
                    default:
                        System.err.println("Unknown question type received");
                }
            default:
                System.err.println("Unknown messageType received.");
        }
    }

    @Override
    public String toString() {
        return "MessageReceived{" +
                "messageType='" + messageType + '\'' +
                ", status='" + status + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", authenticationType='" + authenticationType + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", sessionToken='" + sessionToken + '\'' +
                '}';
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

}

package com.noomtech.chatserver.handler;

import com.google.gson.Gson;
import com.noomtech.chatserver.model.inboundonly.*;
import com.noomtech.chatserver.model.conversation.*;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


/**
 * Basic web-socket server for the react chat application.
 * @todo - The conversation update messages have to have the escape characters removed.  I think these are there because of how the message is
 * processed by the server framework, as it doesn't contain these characters when it's built in the front-end.  This needs fixing.
 * @author Joshua Newman, September 2024
 */
public class WebSocketHandler extends TextWebSocketHandler {



    private static final String REPLACE_ESCAPED_QUOTES = Pattern.quote("\\\"");
    private static final String QUOTE = "\"";
    private static final String COLON = ":";

    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();

    private enum CONVERSATION_UPDATE_TYPES {

        NEW(0),
        MESSAGE_POSTED(1),
        PARTICIPANT_LEFT_CONVERSATION(2),
        CONVERSATION_ENDED_USER_LEFT(3);

        private final int code;
        CONVERSATION_UPDATE_TYPES(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Session: " + session + " established");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        if (session.isOpen()) {
            //Inbound messages are in the format of "{type: from: payload:}" where "from" is the contact id of the sender, and 'payload' value is the JSON object e.g.
            // "{"type":"LoginRequest","from":"50","payload":{}"
            var messageString = message.getPayload();
            System.out.println("Received: " + messageString);
            //Get the type, who it's from, and the payload
            int firstQuoteIndex = messageString.indexOf("\"");
            int secondQuoteIndex = messageString.substring(firstQuoteIndex + 1).indexOf(QUOTE);
            var type = messageString.substring(firstQuoteIndex + 1, secondQuoteIndex);
            int thirdQuoteIndex = messageString.substring(secondQuoteIndex + 1).indexOf(QUOTE);
            int fourthQuoteIndex = messageString.substring(thirdQuoteIndex + 1).indexOf(QUOTE);
            var fromString = messageString.substring(thirdQuoteIndex + 1, fourthQuoteIndex);
            var from = Long.parseLong(fromString);
            int colonIndexBeforePayload = messageString.substring(fourthQuoteIndex + 1).indexOf(COLON);
            final String payloadString = messageString.substring(colonIndexBeforePayload + 1, messageString.length() - 1).replaceAll(REPLACE_ESCAPED_QUOTES, QUOTE);

            if(!type.equals("LoginRequest")) {

                if(!sessions.containsKey(from)) {
                    System.out.println("Session " + session.getUri() + " for user: " + fromString + " has not logged in");
                }
                else {

                    switch (type) {
                        case "FullSnapshotRequest": {
                            //@todo - build the Conversations object from the db and send it
                            break;
                        }
                        case "NewConversationRequest": {
                            var conversationRequestObject = convertToObject(payloadString, NewConversationRequest.class);
                            //@todo - generate conversation Id from db (different DB object?), find participant details from search term, add new conversation to db
                            var participantIds = new Long[0];
                            var newConversation = new Conversation(1l, new ConversationParticipant[]{new ConversationParticipant("Simon", "Balls", 4L)}, new Message[]{}, "");
                            sendToUsers(new TextMessage("{\"type\":\"ConversationUpdate\", \"updateType\":\"" + CONVERSATION_UPDATE_TYPES.NEW +
                                    "\", \"conversation\":" + GSON.toJson(newConversation) + "}"), participantIds);

                        }
                        case "UserPostedInConversationNotification": {
                            var userPostedObject = convertToObject(payloadString, UserPostedInConversationNotification.class);
                            var newMessage = new Message(userPostedObject.getText(), userPostedObject.getWhen(), userPostedObject.getConversationId());
                            //@todo update db
                            //@todo - Look up participants in conversation and get a list of ids back
                            var participants = new Long[0];
                            sendToUsers(new TextMessage("{\"type\":\"ConversationUpdate\", \"updateType\":\"" + CONVERSATION_UPDATE_TYPES.MESSAGE_POSTED +
                                    "\", \"conversation\":" + GSON.toJson(newMessage) + "}"), participants);
                            break;
                        }
                        case "UserLeftConversationNotification": {
                            var userLeftConversationObject = convertToObject(payloadString, UserLeftConversationNotification.class);
                            //@todo - update db, get list of participants
                            boolean conversationEnded = false;
                            var userWhoLeft = userLeftConversationObject.getUserId();
                            var participants = new Long[0];
                            if(conversationEnded) {
                                sendToUsers(new TextMessage("{\"type\":\"ConversationUpdate\", \"updateType\":\"" + CONVERSATION_UPDATE_TYPES.CONVERSATION_ENDED_USER_LEFT +
                                        "\",\"user\":\"" + userWhoLeft + "\"}"), participants);
                            }
                            else {
                                sendToUsers(new TextMessage("{\"type\":\"ConversationUpdate\", \"updateType\":\"" + CONVERSATION_UPDATE_TYPES.PARTICIPANT_LEFT_CONVERSATION +
                                        "\",\"user\":\"" + userWhoLeft + "\"}"), participants);
                            }
                        }
                        case "Logout": {
                            var logoutRequest = convertToObject(payloadString, LogoutRequest.class);
                            System.out.println("Logging out user: " + logoutRequest);
                            var loggedOutSession = sessions.remove(logoutRequest.getUserId());
                            if(loggedOutSession == null) {
                                System.out.println("ERROR: user was never logged in!");
                            }
                            else {
                                loggedOutSession.close();
                            }
                        }
                        default: {
                            System.out.println("Unknown message type: " + type);
                        }
                    }
                }
            }
            else {
                LoginRequest loginRequest = convertToObject(payloadString, LoginRequest.class);
                //@todo - do username and pwd checks
                long userId = -1L;
                var userName = "simon";
                if(userId > -1) {
                    System.out.println("User " + userName + " logged in successfully");
                    sessions.put(userId, session);
                }
                else {
                    System.out.println("User " + userName + " failed login");
                }
                send(session, new TextMessage("{\"userId\":\"" + userId + "\"}"));
            }
        } else {
            System.out.println("Websocket not open on session: " + session.getUri());
        }


    }

    private static <R> R convertToObject(String json, Class<R> cl) {
        return GSON.fromJson(json, cl);
    }

    private void send(WebSocketSession session, TextMessage msg) {
        System.out.println("Sending: " + msg.getPayload());
        try {
            session.sendMessage(msg);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToUsers(TextMessage message, Long[] userIds) {
        for(Long userId : userIds) {
            Optional.ofNullable(sessions.get(userId)).ifPresent(session -> {send(session, message);});
        }
    }
}
package com.noomtech.chatserver.handler;

import com.google.gson.Gson;
import com.noomtech.chatserver.model.inboundonly.*;
import com.noomtech.chatserver.model.conversation.*;
import com.noomtech.chatserver.utilities.ChatDataRepository;
import com.noomtech.chatserver.utilities.ChatDataRepositoryImplDB;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.UUID;


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

    private static final Map<UUID, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Gson GSON = new Gson();
    
    private final ChatDataRepository chatDataRepository;

    private enum CONVERSATION_UPDATE_TYPES {

        NEW,
        MESSAGE_POSTED,
        PARTICIPANT_LEFT_CONVERSATION,
        CONVERSATION_ENDED_USER_LEFT;
    }

    
    public WebSocketHandler() {
        chatDataRepository = ChatDataRepositoryImplDB.getInstance();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        System.out.println("Session: " + session + " established");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        if (session.isOpen()) {
            //Inbound messages are in the format of "{type: from: payload:}" where "from" is the user id of the sender, and 'payload' is the JSON object e.g.
            // "{"type":"LoginRequest","from":"52jh3erw0fejue90","payload":{}"
            var messageString = message.getPayload();
            System.out.println("Received: " + messageString);
            //Get the type, who it's from, and the payload
            int firstQuoteIndex = messageString.indexOf("\"");
            int secondQuoteIndex = messageString.substring(firstQuoteIndex + 1).indexOf(QUOTE);
            var type = messageString.substring(firstQuoteIndex + 1, secondQuoteIndex);
            int thirdQuoteIndex = messageString.substring(secondQuoteIndex + 1).indexOf(QUOTE);
            int fourthQuoteIndex = messageString.substring(thirdQuoteIndex + 1).indexOf(QUOTE);
            var fromString = messageString.substring(thirdQuoteIndex + 1, fourthQuoteIndex);
            var from = UUID.fromString(fromString);
            int colonIndexBeforePayload = messageString.substring(fourthQuoteIndex + 1).indexOf(COLON);
            final String payloadString = messageString.substring(colonIndexBeforePayload + 1, messageString.length() - 1).replaceAll(REPLACE_ESCAPED_QUOTES, QUOTE);

            if(!type.equals("LoginRequest")) {

                if(!sessions.containsKey(from)) {
                    System.out.println("Session " + session.getUri() + " for user: " + fromString + " has not logged in");
                }
                else {

                    switch (type) {
                        case "FullSnapshotRequest": {
                            var conversationsForUser = chatDataRepository.getConversationsForUser(from);
                            send(session, new TextMessage("{\"type\":\"FullSnapshotResponse\", \"conversations\":" +GSON.toJson(conversationsForUser) + "}"));
                            break;
                        }
                        case "NewConversationRequest": {
                            var conversationRequestObject = convertToObject(payloadString, NewConversationRequest.class);
                            var newConversationServerOnly = chatDataRepository.addNewConversation(conversationRequestObject.name(), conversationRequestObject.firstNames(), conversationRequestObject.lastNames());
                            var newConversationForClients = new Conversation(
                                    newConversationServerOnly.id(),
                                    newConversationServerOnly.dateStarted(),
                                    (ConversationParticipant[])Arrays.stream(newConversationServerOnly.conversationParticipants()).map(
                                            c -> new ConversationParticipant(c.firstName(), c.lastName(), c.participantId())).toArray(),
                                    newConversationServerOnly.messages(),
                                    newConversationServerOnly.draftedMessage(),
                                    newConversationServerOnly.name());
                            sendToUsers(new TextMessage(
                                    "{\"type\":\"ConversationUpdate\", " +
                                    "\"updateType\":\"" + CONVERSATION_UPDATE_TYPES.NEW +
                                    "\", \"conversation\":" + GSON.toJson(newConversationForClients) + "}"),
                                    (UUID[])Arrays.stream(newConversationServerOnly.conversationParticipants()).map(ConversationParticipantServerOnly::userId).toArray());

                        }
                        case "UserPostedInConversationNotification": {
                            var userPostedObject = convertToObject(payloadString, UserPostedInConversationNotification.class);
                            var newMessage = new Message(userPostedObject.text(), userPostedObject.when(), userPostedObject.participantId());
                            chatDataRepository.addNewMessage(newMessage);
                            var usersInConversation = chatDataRepository.getUserIdsInConversationForParticipantId(userPostedObject.participantId());
                            sendToUsers(new TextMessage("{\"type\":\"ConversationUpdate\", \"updateType\":\"" + CONVERSATION_UPDATE_TYPES.MESSAGE_POSTED +
                                    "\", \"conversation\":" + GSON.toJson(newMessage) + "}"), usersInConversation);
                            break;
                        }
                        case "UserLeftConversationNotification": {
                            var userLeftConversationObject = convertToObject(payloadString, UserLeftConversationNotification.class);
                            var userWhoLeft = userLeftConversationObject.participantId();
                            boolean conversationEnded = chatDataRepository.removeParticipantFromConversation(userWhoLeft);
                            var participants = new UUID[0];
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
                            var loggedOutSession = sessions.remove(logoutRequest.userId());
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
                var userId = chatDataRepository.checkLogin(loginRequest.username(), loginRequest.password());
                if(userId != null) {
                    System.out.println("User " + loginRequest.username() + " logged in successfully");
                    sessions.put(userId, session);
                    send(session, new TextMessage("{\"type\":\"LoginSuccessful\", \"userId\":\"" + userId + "\"}"));
                }
                else {
                    System.out.println("User " + loginRequest.username() + " failed login");
                    send(session, new TextMessage("{\"type\":\"LoginFailed\"}"));
                }
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

    private void sendToUsers(TextMessage message, UUID[] userIds) {
        for(UUID userId : userIds) {
            Optional.ofNullable(sessions.get(userId)).ifPresent(session -> {send(session, message);});
        }
    }
}
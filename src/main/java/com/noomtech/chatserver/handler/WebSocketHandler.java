package com.noomtech.chatserver.handler;

import com.noomtech.chatserver.model.ServerData;
import com.noomtech.chatserver.model.contact.Contact;
import com.noomtech.chatserver.model.contact.ContactDetails;
import com.noomtech.chatserver.model.conversation.Conversation;
import com.noomtech.chatserver.model.conversation.ConversationDetails;
import com.noomtech.chatserver.model.conversation.Message;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.google.gson.Gson;
import org.springframework.web.socket.handler.TextWebSocketHandler;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Basic web-socket server for the react chat application.
 * @todo - The conversation update messages have to have the escape characters removed.  I think these are there because of how the message is
 * processed by the server framework, as it doesn't contain these characters when it's built in the front-end.  This needs fixing.
 * @author Joshua Newman, September 2024
 */
public class WebSocketHandler extends TextWebSocketHandler {

    //The starting data
    private static final ServerData INITIAL_DATA;
    static {
        var cd1 = new ConversationDetails(new Message[]{new Message("J")}, "J");
        var cd2 = new ConversationDetails(new Message[]{new Message("B")}, "B");
        var cd3 = new ConversationDetails(new Message[]{new Message("C")}, "C");
        var cd4 = new ConversationDetails(new Message[]{new Message("A")}, "A");
        var conversation1 = new Conversation(0L, cd1);
        var conversation2 = new Conversation(1L, cd2);
        var conversation3 = new Conversation(2L, cd3);
        var conversation4 = new Conversation(3L, cd4);
        var conversations = new Conversation[]{conversation1, conversation2, conversation3, conversation4};


        var c1 = new ContactDetails("Joshua", "Newman");
        var c2 = new ContactDetails("Bill", "Plums");
        var c3 = new ContactDetails("Clementine", "Flapcock");
        var c4 = new ContactDetails("Audrey", "Scrollard");
        var contact1 = new Contact(0L, c1);
        var contact2 = new Contact(1L, c2);
        var contact3 = new Contact(2L, c3);
        var contact4 = new Contact(3L, c4);
        var contacts = new Contact[]{contact1, contact2, contact3, contact4};

        INITIAL_DATA = new ServerData(contacts, conversations);
    }

    private static final Set<WebSocketSession> sessions = new HashSet<>();
    private static final String REPLACE_ESCAPED_QUOTES = Pattern.quote("\\\"");
    private static final Gson GSON = new Gson();


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {

        //Inbound messages are in the format of "type=... payload=..." where the 'payload' value is the JSON object

        for (WebSocketSession webSocketSession : sessions) {
            if (webSocketSession.isOpen()) {
                try {

                    var messageString = message.getPayload();
                    System.out.println("Received: " + messageString);
                    //Get the type and the payload
                    int firstSpaceIndex = messageString.indexOf(" ");
                    var type = messageString.substring(6, firstSpaceIndex);
                    String payloadString = messageString.substring(firstSpaceIndex + 1);
                    payloadString = payloadString.substring(0, payloadString.length() - 1);

                    switch (type) {
                        case "RequestFullSnapshot": {
                            send(session, new TextMessage("{\"type\" : \"FullSnapshot\", \"data\" : " + GSON.toJson(INITIAL_DATA) + "}"));
                            break;
                        }
                        case "ConversationUpdate": {
                            payloadString = payloadString.replaceAll(REPLACE_ESCAPED_QUOTES, "\"");
                            Conversation conversation = GSON.fromJson(payloadString, Conversation.class);
                            send(session, new TextMessage("{\"type\":\"ConversationUpdate\", \"conversation\":" + GSON.toJson(conversation) + "}"));
                            break;
                        }
                        default: {
                            System.out.println("Unknown message type: " + type);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Websocket not open");
            }

        }
    }

    private void send(WebSocketSession session, TextMessage msg) throws IOException {
        System.out.println("Sending: " + msg.getPayload());
        session.sendMessage(msg);
    }
}
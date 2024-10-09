package com.noomtech.chatserver.model.conversation;

public class Conversation {

    private final long id;
    private final ConversationParticipant[] conversationParticipants;
    private final Message[] messages;
    private final String draftedMessage;


    public Conversation(long id, ConversationParticipant[] conversationParticipants, Message[] messages, String draftedMessage) {
        this.id = id;
        this.conversationParticipants = conversationParticipants;
        this.messages = messages;
        this.draftedMessage = draftedMessage;
    }

    public long getId() {
        return id;
    }

    public ConversationParticipant[] getConversationParticipants() {
        return conversationParticipants;
    }

    public Message[] getMessages() {
        return messages;
    }

    public String getDraftedMessage() {
        return draftedMessage;
    }
}





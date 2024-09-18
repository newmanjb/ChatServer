package com.noomtech.chatserver.model.conversation;

public class Conversation {

    private final long id;
    private final ConversationDetails conversationDetails;

    public Conversation(long id, ConversationDetails conversationDetails) {
        this.id = id;
        this.conversationDetails = conversationDetails;
    }

    public ConversationDetails getConversationDetails() {
        return conversationDetails;
    }

    public long getId() {
        return id;
    }
}

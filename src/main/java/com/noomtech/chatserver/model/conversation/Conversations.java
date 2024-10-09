package com.noomtech.chatserver.model.conversation;

public class Conversations {


    private final Conversation[] converversations;

    public Conversations(Conversation[] converversations) {
        this.converversations = converversations;
    }

    public Conversation[] getConverversations() {
        return converversations;
    }
}

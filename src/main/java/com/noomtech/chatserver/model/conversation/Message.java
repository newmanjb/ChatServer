package com.noomtech.chatserver.model.conversation;

public class Message {

    private String text;
    private String dateSent;
    private long conversationId;

    public Message(String text, String dateSent, long conversationId) {
        this.text = text;
        this.dateSent = dateSent;
        this.conversationId = conversationId;
    }

    public String getText() {
        return text;
    }

    public String getDateSent() {
        return dateSent;
    }

    public long getConversationId() {
        return conversationId;
    }
}

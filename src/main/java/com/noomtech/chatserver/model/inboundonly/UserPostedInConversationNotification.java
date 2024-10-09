package com.noomtech.chatserver.model.inboundonly;

public class UserPostedInConversationNotification {


    private String text;
    private String when;
    private long conversationId;

    public UserPostedInConversationNotification(String text, String when, long conversationId) {
        this.text = text;
        this.when = when;
        this.conversationId = conversationId;
    }

    public String getText() {
        return text;
    }

    public String getWhen() {
        return when;
    }

    public long getConversationId() {
        return conversationId;
    }
}

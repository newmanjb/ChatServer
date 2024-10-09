package com.noomtech.chatserver.model.inboundonly;

public class UserLeftConversationNotification {

    private final Long userId;
    private final Long conversationId;


    public UserLeftConversationNotification(Long userId, Long conversationId) {
        this.userId = userId;
        this.conversationId = conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getConversationId() {
        return conversationId;
    }
}

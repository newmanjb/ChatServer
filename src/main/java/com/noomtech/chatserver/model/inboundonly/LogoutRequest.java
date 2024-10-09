package com.noomtech.chatserver.model.inboundonly;

public class LogoutRequest {

    private final long userId;

    public LogoutRequest(long userId) {
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }
}

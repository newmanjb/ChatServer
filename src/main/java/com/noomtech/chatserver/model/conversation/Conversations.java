package com.noomtech.chatserver.model.conversation;

import java.util.Arrays;

public record Conversations(Conversation[] conversations) {

    @Override
    public String toString() {
        return Arrays.toString(conversations);
    }
}

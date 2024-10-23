package com.noomtech.chatserver.model.conversation;

import java.util.Arrays;
import java.util.UUID;

public record Conversation(UUID id, long dateStarted, ConversationParticipant[] conversationParticipants,
                           Message[] messages, String draftedMessage, String name) {

    @Override
    public String toString() {
        return  "id=" + id +
                " dateStarted=" + dateStarted +
                " conversation participants=" + Arrays.toString(conversationParticipants) +
                " messages=" + Arrays.toString(messages) +
                " drafted message=" + draftedMessage +
                " name=" + name;
    }
}





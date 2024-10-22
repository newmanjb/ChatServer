package com.noomtech.chatserver.model.conversation;

import java.util.UUID;

public record Conversation(UUID id, long dateStarted, ConversationParticipant[] conversationParticipants,
                           Message[] messages, String draftedMessage, String name) {

}





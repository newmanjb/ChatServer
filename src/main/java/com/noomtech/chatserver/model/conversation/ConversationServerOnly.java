package com.noomtech.chatserver.model.conversation;

import java.util.UUID;

public record ConversationServerOnly(UUID id, long dateStarted,
                                     ConversationParticipantServerOnly[] conversationParticipants, Message[] messages,
                                     String draftedMessage, String name) {

}





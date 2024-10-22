package com.noomtech.chatserver.model.conversation;

import java.util.UUID;

public record ConversationParticipantServerOnly(String firstName, String lastName, UUID participantId, UUID userId) {

}

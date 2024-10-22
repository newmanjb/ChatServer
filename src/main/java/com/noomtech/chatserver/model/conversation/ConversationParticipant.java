package com.noomtech.chatserver.model.conversation;

import java.util.UUID;


public record ConversationParticipant(String firstName, String lastName, UUID participantId){}
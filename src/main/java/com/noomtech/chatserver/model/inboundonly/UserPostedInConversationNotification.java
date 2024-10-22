package com.noomtech.chatserver.model.inboundonly;

import java.util.UUID;


public record UserPostedInConversationNotification(String text, long when, UUID participantId){}
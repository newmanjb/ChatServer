package com.noomtech.chatserver.model.conversation;

import java.util.UUID;

public record Message(String text, long timeSent, UUID participantId) {

}

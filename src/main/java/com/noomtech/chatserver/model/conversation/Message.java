package com.noomtech.chatserver.model.conversation;

import java.util.UUID;

public record Message(String text, long timeSent, UUID messageID, UUID participantId) {


    public boolean equals(Object other) {
        if(other == null  || other.getClass() != getClass() || messageID == null) {
            return false;
        }
        return messageID.equals(((Message)other).messageID);
    }
}

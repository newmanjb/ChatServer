package com.noomtech.chatserver.model.conversation;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public record Conversation(UUID id, long dateStarted, ConversationParticipant[] conversationParticipants,
                           Message[] messages, String draftedMessage, String name) {

    @Override
    public String toString() {
        return "Conversation{" +
                "id=" + id +
                ", dateStarted=" + dateStarted +
                ", conversationParticipants=" + Arrays.toString(conversationParticipants) +
                ", messages=" + Arrays.toString(messages) +
                ", draftedMessage='" + draftedMessage + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Conversation that = (Conversation) o;
        if(this.id != null) {
            return this.id.equals(that.id);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}





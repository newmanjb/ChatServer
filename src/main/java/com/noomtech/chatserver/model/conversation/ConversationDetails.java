package com.noomtech.chatserver.model.conversation;

public class ConversationDetails {

    private final Message[] history;
    private final String draftedMessage;

    public ConversationDetails(Message[] history, String draftedMessage) {
        this.history = history;
        this.draftedMessage = draftedMessage;
    }

    public Message[] getHistory() {
        return history;
    }

    public String getDraftedMessage() {
        return draftedMessage;
    }
}

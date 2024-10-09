package com.noomtech.chatserver.model.inboundonly;

public class NewConversationRequest {


    private final String name;
    private final String[] participants;

    public NewConversationRequest(String name, String[] participants) {
        this.name = name;
        this.participants = participants;
    }

    public String getName() {
        return name;
    }

    public String[] getParticipants() {
        return participants;
    }
}

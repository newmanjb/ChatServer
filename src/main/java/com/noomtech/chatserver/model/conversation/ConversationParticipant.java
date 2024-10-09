package com.noomtech.chatserver.model.conversation;

public class ConversationParticipant {

    private String firstName;
    private String lastName;
    private Long userId;

    public ConversationParticipant(String firstName, String lastName, Long userId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Long getUserId() {
        return userId;
    }
}

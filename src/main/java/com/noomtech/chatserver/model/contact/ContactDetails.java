package com.noomtech.chatserver.model.contact;

public class ContactDetails {

    private final String firstName;
    private final String lastName;

    public ContactDetails(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }
}

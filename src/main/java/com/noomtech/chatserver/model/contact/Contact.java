package com.noomtech.chatserver.model.contact;

public class Contact {

    private final long id;
    private final ContactDetails contactDetails;

    public Contact(long id, ContactDetails contactDetails) {
        this.id = id;
        this.contactDetails = contactDetails;
    }

    public long getId() {
        return id;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }
}

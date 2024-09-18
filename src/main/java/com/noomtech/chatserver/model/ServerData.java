package com.noomtech.chatserver.model;

import com.noomtech.chatserver.model.contact.Contact;
import com.noomtech.chatserver.model.conversation.Conversation;

public class ServerData {


    private final Contact[] contacts;
    private final Conversation[] conversations;


    public ServerData(Contact[] contacts, Conversation[] conversations) {
        this.contacts = contacts;
        this.conversations = conversations;
    }


    public Conversation[] getConversations() {
        return conversations;
    }

    public Contact[] getContacts() {
        return contacts;
    }
}

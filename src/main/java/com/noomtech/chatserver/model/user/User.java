package com.noomtech.chatserver.model.user;

import java.util.UUID;

public record User(UUID id, String firstName, String lastName, String username, int age) {

}

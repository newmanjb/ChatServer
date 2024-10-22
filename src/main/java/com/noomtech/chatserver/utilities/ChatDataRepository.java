package com.noomtech.chatserver.utilities;

import com.noomtech.chatserver.model.conversation.ConversationServerOnly;
import com.noomtech.chatserver.model.conversation.Conversations;
import com.noomtech.chatserver.model.conversation.Message;
import com.noomtech.chatserver.model.user.User;

import java.util.UUID;

public interface ChatDataRepository extends AutoCloseable {

    Conversations getConversationsForUser(UUID userId) throws Exception;

    User getUser(String firstName, String lastName) throws Exception;

    ConversationServerOnly addNewConversation(String conversationName, String[] participantFirstNames, String[] participantLastNames) throws Exception;

    void addNewMessage(Message message) throws Exception;

    UUID[] getUserIdsInConversationForParticipantId(UUID participantId) throws Exception;

    boolean removeParticipantFromConversation(UUID participantId) throws Exception;

    UUID checkLogin(String uName, String pwd) throws Exception;

    void close();
}

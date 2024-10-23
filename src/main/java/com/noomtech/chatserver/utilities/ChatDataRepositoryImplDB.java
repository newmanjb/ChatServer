package com.noomtech.chatserver.utilities;

import com.noomtech.chatserver.model.conversation.*;
import com.noomtech.chatserver.model.user.User;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


/**
 * A SQL database implementation of {@link ChatDataRepository}.
 * @author Joshua Newman, October 2024
 */
public class ChatDataRepositoryImplDB implements ChatDataRepository {


    private static final String SINGLE_QUOTE = "'";
    private static final String COMMA = ",";
    private static final String EMPTY_STRING = "";

    private static final String SQL_GET_CONVERSATIONS_FOR_USER =
            "SELECT c.conversation_id, c.date_started, cp.participant_id, c.conversation_name FROM " +
            "conversations c, conversation_participants cp WHERE cp.user_id = ? AND cp.conversation_id = c.conversation_id;";
    private static final String FIRST_NAME_MARKER = "¬1";
    private static final String LAST_NAME_MARKER = "¬2";
    private static final String SQL_GET_USER_IDS_FOR_FULL_NAMES = "SELECT user_id, first_name, last_name FROM users WHERE first_name IN (" + FIRST_NAME_MARKER + ") AND last_name IN (" + LAST_NAME_MARKER + ")";
    private static final String SQL_GET_MESSAGES_IN_CONVERSATION = "SELECT m.msg_text, m.date_sent, m.participant_id FROM messages m, conversation_participants cp " +
            "WHERE m.participant_id = cp.participant_id AND cp.conversation_id = ? ORDER BY m.date_sent DESC";
    private static final String SQL_GET_CONV_PARTICIPANT_DETAILS = "select u.first_name, u.last_name, cp.participant_id FROM users u, conversation_participants cp" +
            " WHERE cp.conversation_id = ? AND cp.user_id = u.user_id";
    private static final String SQL_GET_DRAFTED_MSG_FOR_USER = "SELECT d.msg_text FROM drafted_messages d WHERE d.participant_id = ?";
    private static final String SQL_GET_USER_IDS_IN_CONVERSATION_FOR_PARTICIPANT_ID = "SELECT user_id FROM conversation_participants cp1 WHERE cp1.conversation_id = (SELECT conversation_id FROM conversation_participants cp2 WHERE cp2.participant_id = ?)";
    private static final String SQL_GET_CONVERSATION_DETAILS_FOR_PARTICIPANT_ID = "SELECT conversation_id FROM conversation_participants cp1 WHERE cp1.conversation_id = (SELECT conversation_id FROM conversation_participants cp2 WHERE cp2.participant_id = ?)";
    private static final String SQL_ADD_NEW_MESSAGE = "INSERT INTO messages (message_id, date_sent, participant_id, message_text) VALUES (?,?,?,?)";
    private static final String SQL_ADD_NEW_USER = "INSERT INTO users(user_id, age, first_name, last_name, username, password) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_ADD_CONVERSATION_PARTICIPANTS = "INSERT INTO conversation_participants (conversation_id, user_id, participant_id) VALUES (?,?,?)";
    private static final String SQL_ADD_NEW_CONVERSATION = "INSERT INTO conversations (conversation_id,conversation_name,date_started) VALUES (?, ?, ?)";
    private static final String SQL_REMOVE_PARTICIPANT_FROM_CONV = "DELETE FROM conversation_participants WHERE participant_id = ?";
    private static final String SQL_REMOVE_PARTICIPANTS_FROM_CONVERSATION = "DELETE FROM conversation_participants WHERE conversation_id = ?";
    private static final String SQL_REMOVE_MESSAGES_FOR_CONVERSATION = "DELETE FROM messages m, conversations c WHERE c.conversation_id = ? AND c.participant_id = m.participant_id";
    private static final String SQL_REMOVE_CONVERSATION = "DELETE FROM conversations c WHERE c.conversation_id = ?";
    private static final String SQL_CHECK_LOGIN_DETAILS = "SELECT user_id FROM users WHERE username = ? and password = ?";

    private HikariDataSource CONNECTION_POOL;


    private static final class InstanceHolder {
        private static final ChatDataRepositoryImplDB INSTANCE = new ChatDataRepositoryImplDB();
    }

    public static ChatDataRepository getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private ChatDataRepositoryImplDB() {
        var dbConfig = System.getProperty("DB_CONFIG");
        if(dbConfig == null) {
            throw new IllegalStateException("No value set for env var 'DB_CONFIG'.  Cannot set up db connections");
        }
        var settings = dbConfig.split(",");
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(settings[0]);
            hikariConfig.setUsername(settings[1]);
            hikariConfig.setPassword(settings[2]);
            hikariConfig.setMaximumPoolSize(Integer.parseInt(settings[3]));
            CONNECTION_POOL = new HikariDataSource( hikariConfig );
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Conversations getConversationsForUser(UUID userId) throws SQLException {
        var connection = CONNECTION_POOL.getConnection();
        try(var preparedStatement = connection.prepareStatement(SQL_GET_CONVERSATIONS_FOR_USER)) {
            preparedStatement.setObject(1, userId);
            List<Conversation> conversationList = new ArrayList<>();
            try (var rsConversations = preparedStatement.executeQuery()) {

                while (rsConversations.next()) {
                    var conversationId = rsConversations.getObject(1);
                    var participantId = rsConversations.getObject(3);
                    var name = rsConversations.getString(4);

                    var messagesInConversation = new ArrayList<Message>();
                    var participantsInConversation = new ArrayList<ConversationParticipant>();
                    var draftedMessage = EMPTY_STRING;

                    try (var preparedStatement1 = connection.prepareStatement(SQL_GET_MESSAGES_IN_CONVERSATION);
                         var preparedStatement2 = connection.prepareStatement(SQL_GET_CONV_PARTICIPANT_DETAILS);
                         var preparedStatement3 = connection.prepareStatement(SQL_GET_DRAFTED_MSG_FOR_USER)) {
                        preparedStatement1.setObject(1, conversationId);
                        preparedStatement2.setObject(1, conversationId);
                        preparedStatement3.setObject(1, participantId);

                        try (var messagesRs = preparedStatement1.executeQuery();
                             var participantsRs = preparedStatement2.executeQuery();
                             var draftedMessageRs = preparedStatement3.executeQuery()) {

                            while (messagesRs.next()) {
                                var message = new Message(messagesRs.getString(1), messagesRs.getTimestamp(2).getTime(), (UUID) messagesRs.getObject(3));
                                messagesInConversation.add(message);
                            }

                            while (participantsRs.next()) {
                                var conversationParticipant = new ConversationParticipant(participantsRs.getString(1), participantsRs.getString(2), (UUID) participantsRs.getObject(3));
                                participantsInConversation.add(conversationParticipant);
                            }

                            if (draftedMessageRs.next()) {
                                draftedMessage = draftedMessageRs.getString(1);
                                if (draftedMessageRs.next()) {
                                    throw new IllegalStateException(">1 drafted message found for conversation '" + conversationId + "' with name '" + name + "' for participant '" + participantId + "'");
                                }
                            }
                        }
                    }

                    var dateStarted = rsConversations.getTimestamp(2).getTime();

                    var conversationObject = new Conversation(
                            (UUID) conversationId,
                            dateStarted,
                            participantsInConversation.toArray(new ConversationParticipant[0]),
                            messagesInConversation.toArray(new Message[0]),
                            draftedMessage,
                            name);
                    conversationList.add(conversationObject);
                }
            }
            return new Conversations(conversationList.toArray(new Conversation[0]));
        }
    }

    @Override
    public ConversationServerOnly addNewConversation(String conversationName, String[] participantFirstNames, String[] participantLastNames) throws Exception {

        var conversationId = UUID.randomUUID();
        var connection = CONNECTION_POOL.getConnection();
        boolean originalAutocommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (var preparedStatement1 = connection.prepareStatement(SQL_ADD_NEW_CONVERSATION);
             var preparedStatement2 = connection.prepareStatement(SQL_ADD_CONVERSATION_PARTICIPANTS)) {
            preparedStatement1.setObject(1, conversationId);
            preparedStatement1.setObject(2, conversationName);
            var now = Timestamp.from(Instant.now());
            preparedStatement1.setTimestamp(3, now);
            var r = preparedStatement1.executeUpdate();
            if (r != 1) {
                throw new SQLException("Add conversation sql updated " + r + " rows");
            }

            var conversationParticipants = getUserIdsForFullNames(participantFirstNames, participantLastNames);
            for (var conversationParticipant : conversationParticipants) {
                preparedStatement2.setObject(1, conversationId);
                preparedStatement2.setObject(2, conversationParticipant.userId());
                preparedStatement2.setObject(3, conversationParticipant.participantId());
                preparedStatement2.addBatch();
            }
            preparedStatement2.executeBatch();

            connection.commit();

            return new ConversationServerOnly(
                    conversationId,
                    now.getTime(),
                    conversationParticipants.toArray(new ConversationParticipantServerOnly[0]),
                    new Message[0],
                    EMPTY_STRING,
                    conversationName);
        }
        catch (Exception e) {
            connection.rollback();
            throw e;
        }
        finally {
            connection.setAutoCommit(originalAutocommit);
        }

    }


    @Override
    public void addNewMessage(Message newMessage) throws Exception {

        try(var preparedStatement = CONNECTION_POOL.getConnection().prepareStatement(SQL_ADD_NEW_MESSAGE)) {
            preparedStatement.setObject(1, UUID.randomUUID());
            preparedStatement.setTimestamp(2, new Timestamp(newMessage.timeSent()));
            preparedStatement.setObject(3, newMessage.participantId());
            preparedStatement.setString(4, newMessage.text());
            var updateCount = preparedStatement.executeUpdate();
            if(updateCount != 1) {
                throw new IllegalStateException("Adding new message: " + newMessage + " resulted in " + updateCount + " updates");
            }
        }

    }

    @Override
    public UUID[] getUserIdsInConversationForParticipantId(UUID participantId) throws Exception {
        try(var preparedStatement = CONNECTION_POOL.getConnection().prepareStatement(SQL_GET_USER_IDS_IN_CONVERSATION_FOR_PARTICIPANT_ID)) {
            preparedStatement.setObject(1, participantId);
            try (var rs = preparedStatement.executeQuery()) {
                var userIds = new ArrayList<UUID>();
                while (rs.next()) {
                    userIds.add((UUID) rs.getObject(1));
                }
                return userIds.toArray(new UUID[0]);
            }
        }
    }

    @Override
    public boolean removeParticipantFromConversation(UUID participantId) throws Exception {

        //Removed the entire conversation if this participant is the last one

        var conversationRemoved = false;
        var connection = CONNECTION_POOL.getConnection();
        boolean originalAutocommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try(var preparedStatement1 = connection.prepareStatement(SQL_GET_CONVERSATION_DETAILS_FOR_PARTICIPANT_ID);
            var preparedStatement2 = connection.prepareStatement(SQL_REMOVE_PARTICIPANT_FROM_CONV)) {
            preparedStatement1.setObject(1, participantId);
            try (var rs = preparedStatement1.executeQuery()) {
                int numParticipantsInConversations = 0;
                UUID conversationId = null;
                while (rs.next()) {
                    numParticipantsInConversations++;
                    conversationId = (UUID) rs.getObject(1);
                }
                if (numParticipantsInConversations > 1) {
                    preparedStatement2.setObject(1, participantId);
                    var updated = preparedStatement2.executeUpdate();
                    if (updated != 1) {
                        throw new IllegalArgumentException("Deleting " + participantId + " from conversation returned " + updated);
                    }
                } else if (numParticipantsInConversations == 1) {
                    removeConversation(conversationId);
                    conversationRemoved = true;
                } else {
                    throw new IllegalArgumentException("Number of participants in conversation with id: " + conversationId + " was " + numParticipantsInConversations);
                }
            }
            connection.commit();
            return conversationRemoved;
        }
        catch(Exception e) {
            connection.rollback();
            throw e;
        }
        finally {
            connection.setAutoCommit(originalAutocommit);
        }
    }

    private void removeConversation(UUID conversationId) throws Exception {

        var connection = CONNECTION_POOL.getConnection();
        boolean originalAutocommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try(var preparedStatement1 = connection.prepareStatement(SQL_REMOVE_MESSAGES_FOR_CONVERSATION);
            var preparedStatement2 = connection.prepareStatement(SQL_REMOVE_PARTICIPANTS_FROM_CONVERSATION);
            var preparedStatement3 = connection.prepareStatement(SQL_REMOVE_CONVERSATION)) {

            preparedStatement1.setObject(1, conversationId);
            preparedStatement1.executeUpdate();

            preparedStatement2.setObject(1, conversationId);
            var updated = preparedStatement2.executeUpdate();
            if (updated < 1) {
                throw new IllegalStateException("Deleting participants from conversation " + conversationId + " resulted in " + updated + " updates");
            }

            preparedStatement3.setObject(1, conversationId);
            updated = preparedStatement3.executeUpdate();
            if (updated != 1) {
                throw new IllegalStateException("Deleting conversation " + conversationId + " resulted in " + updated + " updates");
            }

            connection.commit();
        }
        catch(Exception e) {
            connection.rollback();
            throw e;
        }
        finally {
            connection.setAutoCommit(originalAutocommit);
        }
    }

    @Override
    public UUID checkLogin(String uName, String pwd) throws Exception {
        try(var preparedStatement = CONNECTION_POOL.getConnection().prepareStatement(SQL_CHECK_LOGIN_DETAILS)) {
            preparedStatement.setString(1, uName);
            preparedStatement.setString(2, pwd);
            try (var rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return (UUID) rs.getObject(1);
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public void close() {
        try {
            CONNECTION_POOL.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private List<ConversationParticipantServerOnly> getUserIdsForFullNames(String[] firstNames, String[] lastNames) throws Exception {
        StringBuilder sbFirst = new StringBuilder();
        for(int i = 0; i < firstNames.length; i++) {
            sbFirst.append(SINGLE_QUOTE).append(firstNames[i]).append(SINGLE_QUOTE);
            if(i < firstNames.length - 1) {
                sbFirst.append(COMMA);
            }
        }

        StringBuilder sbLast = new StringBuilder();
        for(int i = 0; i < lastNames.length; i++) {
            sbLast.append(SINGLE_QUOTE).append(lastNames[i]).append(SINGLE_QUOTE);
            if(i < firstNames.length - 1) {
                sbLast.append(COMMA);
            }
        }

        var sql = SQL_GET_USER_IDS_FOR_FULL_NAMES.replace(
                FIRST_NAME_MARKER,
                sbFirst.toString()).replace(
                LAST_NAME_MARKER, sbLast.toString());

        var resultsList = new ArrayList<ConversationParticipantServerOnly>();
        try(var stmt = CONNECTION_POOL.getConnection().createStatement();
            var rs = stmt.executeQuery(sql)) {
            while(rs.next()) {
                var participantId = UUID.randomUUID();
                resultsList.add(new ConversationParticipantServerOnly(rs.getString(2), rs.getString(3), participantId, (UUID)rs.getObject(1)));
            }
        }
        return resultsList;
    }

    @Override
    public void addNewUser(User newUser, String password) throws Exception {

        try(var preparedStatement = CONNECTION_POOL.getConnection().prepareStatement(SQL_ADD_NEW_USER)) {
            preparedStatement.setObject(1, newUser.id());
            preparedStatement.setInt(2, newUser.age());
            preparedStatement.setObject(3, newUser.firstName());
            preparedStatement.setString(4, newUser.lastName());
            preparedStatement.setString(5, newUser.username());
            preparedStatement.setString(6, password);
            var updateCount = preparedStatement.executeUpdate();
            if(updateCount != 1) {
                throw new IllegalStateException("Adding new user: " + newUser.username() + " resulted in " + updateCount + " updates");
            }
        }

    }
}
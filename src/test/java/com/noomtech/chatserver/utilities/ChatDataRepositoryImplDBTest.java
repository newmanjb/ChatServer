package com.noomtech.chatserver.utilities;

import com.noomtech.chatserver.model.conversation.*;
import com.noomtech.chatserver.model.user.User;
import com.noomtech.chatserver.tests.utilities.TestUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.Collectors;


//@todo - add a test for draft messages
public class ChatDataRepositoryImplDBTest {


    private static ChatDataRepository CHAT_DATA_REPOSITORY_IMPL_DB;
    private static Connection CONNECTION;


    @BeforeAll
    public static void beforeAll() throws Exception {
        System.setProperty("DB_CONFIG", "jdbc:postgresql://localhost:5432/,postgres,postgres,80");
        CHAT_DATA_REPOSITORY_IMPL_DB = ChatDataRepositoryImplDB.getInstance();
        CONNECTION = DriverManager.getConnection("jdbc:postgresql://localhost:5432/", "postgres", "postgres");
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        var url = ChatDataRepositoryImplDBTest.class.getResource("/sql/ddl/create_objects.sql");
        if(url != null) {
            TestUtilities.runSQLScript(CONNECTION, Files.readString(Path.of(url.toURI())));
        }
        else {
            throw new IllegalStateException("Could not get file for set-up sql");
        }
    }

    @Test
    public void bigTest1() throws Exception {

        ChatDataRepository chatDataRepository = ChatDataRepositoryImplDB.getInstance();
        //Create a set of new users
        User josh = new User(UUID.randomUUID(), "Joshua", "Newman", "newmanjb", 48);
        User liz = new User(UUID.randomUUID(), "Elizabeth", "Newman", "newmaneb", 47);
        User eve = new User(UUID.randomUUID(), "Eve", "Newman", "newmanee", 11);
        User beth = new User(UUID.randomUUID(), "Beth", "Newman", "newmanbb", 9);

        //Add the users
        chatDataRepository.addNewUser(josh, "plums");
        chatDataRepository.addNewUser(liz, "plums");
        chatDataRepository.addNewUser(eve, "plums");
        chatDataRepository.addNewUser(beth, "plums");

        //Check user details were added to db correctly
        checkUserAgainstDB(josh);
        checkUserAgainstDB(liz);
        checkUserAgainstDB(eve);
        checkUserAgainstDB(beth);

        //Check that the login It's functionality functions properly
        assert(chatDataRepository.checkLogin("newmanjb", "plums").equals(josh.id()));
        assert(chatDataRepository.checkLogin("newmanjb", "plums1") == null);
        assert(chatDataRepository.checkLogin("newmanee", "plums1") == null);
        assert(chatDataRepository.checkLogin("d43d3d", "plums") == null);

        //Create 2 new conversations.  One with all users.  One with just 2.
        //Will need to convert them from the server-only objects to the client side objects
        var conversationEveryone = convertConversationServerOnlyToConversation(CHAT_DATA_REPOSITORY_IMPL_DB.addNewConversation("Everyone", new String[]{"Joshua", "Elizabeth", "Eve", "Beth"}, new String[]{"Newman", "Newman", "Newman", "Newman"}));
        var conversationAdults = convertConversationServerOnlyToConversation(CHAT_DATA_REPOSITORY_IMPL_DB.addNewConversation("Adults", new String[]{"Joshua", "Elizabeth"}, new String[]{"Newman", "Newman"}));

        //Check that the details of the conversation objects returned are correct based on what we specified when creating them
        checkConversationDetails(conversationEveryone, "Everyone", new String[]{"Joshua", "Elizabeth", "Eve", "Beth"}, new String[]{"Newman", "Newman", "Newman", "Newman"}, new String[0], "");
        checkConversationDetails(conversationAdults, "Adults", new String[]{"Joshua", "Elizabeth"}, new String[]{"Newman", "Newman"}, new String[0], "");

        //Get the participant ids for the users in each conversation.  We'll need these.
        var participantIdsMapEveryone = Arrays.stream(conversationEveryone.conversationParticipants()).collect(Collectors.toMap(ConversationParticipant::firstName, ConversationParticipant::participantId));
        var participantIdsMapAdults = Arrays.stream(conversationAdults.conversationParticipants()).collect(Collectors.toMap(ConversationParticipant::firstName, ConversationParticipant::participantId));
        var partIdsArrayEveryone = participantIdsMapEveryone.values().toArray(new UUID[0]);
        var partIdsArrayAdults = participantIdsMapAdults.values().toArray(new UUID[0]);

        //Check that all the participant ids are unique across both conversations
        checkAllAreDifferentInArray(partIdsArrayEveryone);
        checkAllAreDifferentInArray(partIdsArrayAdults);
        checkNoneAreTheSame(partIdsArrayEveryone, partIdsArrayAdults);

        //-------Check that the various methods to get the conversation details for a user function correctly---------------

        //Get user ids of participants - Use Eve's participant id, but we could just as easily user any of the other user's participant ids
        Set<UUID> userIdsOfParticipantsInConversationEveryone = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Eve"))).collect(Collectors.toSet());
        assert(userIdsOfParticipantsInConversationEveryone.size() == 4);
        assert(userIdsOfParticipantsInConversationEveryone.contains(josh.id()));
        assert(userIdsOfParticipantsInConversationEveryone.contains(liz.id()));
        assert(userIdsOfParticipantsInConversationEveryone.contains(eve.id()));
        assert(userIdsOfParticipantsInConversationEveryone.contains(beth.id()));
        Set<UUID> userIdsOfEveryoneInConversationAdults = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Joshua"))).collect(Collectors.toSet());
        assert(userIdsOfEveryoneInConversationAdults.size() == 2);
        assert(userIdsOfEveryoneInConversationAdults.contains(josh.id()));
        assert(userIdsOfEveryoneInConversationAdults.contains(liz.id()));

        //Get conversation details for users
        Map<String, Conversation> conversationsForJosh = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations()).collect(Collectors.toMap(Conversation::name, c-> c));
        assert(conversationsForJosh.size() == 2);
        assert(compareConversations(conversationEveryone, conversationsForJosh.get("Everyone")));
        assert(compareConversations(conversationAdults, conversationsForJosh.get("Adults")));
        Conversations conversationsForEve = CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(eve.id());
        assert(conversationsForEve.conversations().length == 1);
        assert(compareConversations(conversationEveryone, conversationsForEve.conversations()[0]));

        //------------------------------------------------------------------------------------------------------------------

        //--------Send some messages to conversation with everyone and check they were persisted with the conversation----------------------------------------------------------------------
        long now = System.currentTimeMillis();
        var message0 = new Message("Hi everyone", now - 100, UUID.randomUUID(), participantIdsMapEveryone.get("Joshua"));
        var message1 = new Message("Hi Josh", now - 80, UUID.randomUUID(), participantIdsMapEveryone.get("Eve"));
        var message2 = new Message("Hello Josh", now - 60, UUID.randomUUID(), participantIdsMapEveryone.get("Elizabeth"));
        var message3 = new Message("Morning Josh", now - 40, UUID.randomUUID(), participantIdsMapEveryone.get("Beth"));
        var message4 = new Message("How are we doing this morning?", now - 20, UUID.randomUUID(), participantIdsMapEveryone.get("Joshua"));
        var message5 = new Message("Eve - are you ready for school?", now - 10, UUID.randomUUID(), participantIdsMapEveryone.get("Joshua"));
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message0);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message1);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message2);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message3);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message4);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message5);

        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations()[0].messages());

        //----------------------------------------------------------------------------------------------------------------------

        //-------Remove users from conversation with everyone------------------------------
        var message6= new Message("Yes.  Byeee", now - 9, UUID.randomUUID(), participantIdsMapEveryone.get("Eve"));
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message6);
        //The call should return false, as there are still users left in the conversation
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Eve")));
        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5, message6},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations()[0].messages());
        //Should only be 3 users in there now
        var userIds1 = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Beth"))).collect(Collectors.toSet());
        assert(userIds1.contains(josh.id()));
        assert(userIds1.contains(beth.id()));
        assert(userIds1.contains(liz.id()));
        //Eve should not be a member of any conversations
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(eve.id()).conversations().length == 0);
        //The other users will still be part of it
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations().length == 2);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations().length == 2);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations().length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations()[0].name().equals("Everyone"));
        //This call should now only result in 3 users
        var set1 = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Elizabeth"))).collect(Collectors.toSet());
        assert(set1.size() == 3);
        assert(set1.contains(josh.id()));
        assert(set1.contains(beth.id()));
        assert(set1.contains(liz.id()));

        //Remove 2 more users
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Joshua")));
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Beth")));

        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations().length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations()[0].name().equals("Adults"));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations().length == 0);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Elizabeth")).length == 1) &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Elizabeth"))[0].equals(liz.id());

        //Check that the messages are unaffected using the last user's participant id
        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5, message6},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations()[0].messages());

        //Finally, send a message from the last user and then remove them, which should result in the entire conversation being removed
        var message7 = new Message("Over and out", now - 8, UUID.randomUUID(), participantIdsMapEveryone.get("Elizabeth"));
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message7);
        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5, message6, message7},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations()[0].messages());

        //The call should return true this time
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Elizabeth")));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations().length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations()[0].name().equals("Adults"));
        //Check that the entire conversation has been removed from the db
        checkConversationHasBeenRemoved(conversationEveryone.id(), partIdsArrayEveryone);
        //This should now return an empty array
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Elizabeth")).length == 0);

        //-----------------------------------------------------------------

        //-------------Send some message to the "adults" conversation and remove participants------------------

        //Sanity check this conversation to make sure it's unaffected by the happenings above with the other conversation

        compareConversations(conversationAdults, CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations()[0]);
        compareConversations(conversationAdults, CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations()[0]);

        //Send some messages
        var message11 = new Message("Hi Liz", now - 2, UUID.randomUUID(), participantIdsMapAdults.get("Joshua"));
        var message12 = new Message("Afternoon Josh", now - 1, UUID.randomUUID(), participantIdsMapAdults.get("Elizabeth"));
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message11);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message12);
        
        compareMessages(new Message[]{message11, message12}, CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations()[0].messages());

        //Remove both users
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapAdults.get("Joshua")));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations().length == 0);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Elizabeth")).length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Elizabeth"))[0].equals(liz.id()));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapAdults.get("Elizabeth")));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Elizabeth")).length == 0);
        checkConversationHasBeenRemoved(conversationAdults.id(), partIdsArrayAdults);

        //------------------------------------------------------------------------------------------------------
    }


    @AfterAll
    public static void afterAll() {
        Optional.ofNullable(CHAT_DATA_REPOSITORY_IMPL_DB).ifPresent(ChatDataRepository::close);
    }


    private static void checkConversationHasBeenRemoved(UUID conversationId, UUID[] participantIds) throws Exception {
        var sql1 = "SELECT COUNT(conversation_id) FROM conversations WHERE conversation_id = ?";
        var sql2 = "SELECT COUNT(conversation_id) FROM conversation_participants WHERE conversation_id = ?";
        var sql3 = "SELECT COUNT(participant_id) FROM messages WHERE participant_id IN (";

        try(var statement1 = CONNECTION.prepareStatement(sql1);
            var statement2 = CONNECTION.prepareStatement(sql2)) {

            statement1.setObject(1, conversationId);
            statement2.setObject(1, conversationId);

            var sb1 = new StringBuilder(sql3);
            for(int i = 0; i < participantIds.length; i++) {
                sb1.append( i < participantIds.length - 1 ? "?," : "?)");
            }
            sql3 = sb1.toString();
            try(var statement3 = CONNECTION.prepareStatement(sql3)) {
                for (int i = 0; i < participantIds.length; i++) {
                    statement3.setObject(i + 1, participantIds[i]);
                }

                try (var rs1 = statement1.executeQuery();
                     var rs2 = statement2.executeQuery();
                     var rs3 = statement3.executeQuery()) {
                    rs1.next();
                    assert (rs1.getInt(1) == 0);
                    rs2.next();
                    assert (rs2.getInt(1) == 0);
                    rs3.next();
                    assert (rs3.getInt(1) == 0);
                }
            }
        }
    }

    private static void checkAllAreDifferentInArray(Object[] a) {

        for(int i = 0; i < a.length; i++) {
            var sourceObject = a[i];
            for(int j = i+1; j < a.length; j++) {
                assert(!sourceObject.equals(a[j]));
            }
        }
    }

    private static void checkNoneAreTheSame(Object[] a, Object[] b) {
        var longer = a.length > b.length ? a : b;
        var shorter = a == longer ? b : a;
        for (Object o : shorter) {
            for (Object object : longer) {
                assert (!o.equals(object));
            }
        }
    }

    private static void checkUserAgainstDB(User user) throws Exception {
        try(var statement = CONNECTION.prepareStatement("SELECT user_id, first_name, last_name, age, username, password FROM users where user_id = ?")) {
            statement.setObject(1, user.id());
            try(var rs = statement.executeQuery()) {

                assert (rs.next());
                assert ((rs.getObject(1)).equals(user.id()));
                assert (rs.getString(2).equals(user.firstName()));
                assert (rs.getString(3).equals(user.lastName()));
                assert (rs.getInt(4) == user.age());
                assert (rs.getString(5).equals(user.username()));
            }
        }
    }

    private static boolean compareConversations(Conversation conversation1, Conversation conversation2) throws Exception {

        if( conversation2.dateStarted() == conversation1.dateStarted() &&
                Objects.equals(conversation2.id(), conversation1.id()) &&
                Objects.equals(conversation2.name(), conversation1.name()) &&
                Objects.equals(conversation2.draftedMessage(), conversation1.draftedMessage())) {

            assert(conversation1.conversationParticipants().length == conversation2.conversationParticipants().length);
            Arrays.sort(conversation1.conversationParticipants(), new Comparator<ConversationParticipant>() {
                public int compare(ConversationParticipant c1, ConversationParticipant c2) {
                    return c1.participantId().compareTo(c2.participantId());
                }
            });

            Arrays.sort(conversation2.conversationParticipants(), new Comparator<ConversationParticipant>() {
                public int compare(ConversationParticipant c1, ConversationParticipant c2) {
                    return c1.participantId().compareTo(c2.participantId());
                }
            });

            for(int i = 0; i < conversation1.conversationParticipants().length ; i++) {
                ConversationParticipant cp1 = conversation1.conversationParticipants()[i];
                ConversationParticipant cp2 = conversation2.conversationParticipants()[i];
                assert(cp1.firstName().equals(cp2.firstName()) && cp1.lastName().equals(cp2.lastName()) && cp1.participantId().equals(cp2.participantId()));
            }

            assert(conversation1.messages().length == conversation2.messages().length);
            Arrays.sort(conversation1.messages(), new Comparator<Message>() {
                public int compare(Message m1, Message m2) {
                    return m1.text().compareTo(m2.text());
                }
            });

            Arrays.sort(conversation2.messages(), new Comparator<Message>() {
                public int compare(Message m1, Message m2) {
                    return m1.text().compareTo(m2.text());
                }
            });

            for(int i = 0; i < conversation2.messages().length; i++) {
                var m1 = conversation1.messages()[i];
                var m2 = conversation2.messages()[i];
                assert(m1.participantId().equals(m2.participantId()));
                assert(m1.text().equals(m2.text()));
                assert(m1.timeSent() == m2.timeSent());
                assert(m1.messageID() == m2.messageID());
            }

            return true;
        }

        return false;
    }

    private static void checkConversationDetails(
            Conversation compareTo,
            String name,
            String[] participantFirstNames,
            String[] participantLastNames,
            String[] messages,
            String draftedMessage) {

        assert(compareTo.name().equals(name));
        assert(participantFirstNames.length == compareTo.conversationParticipants().length);
        assert(participantLastNames.length == compareTo.conversationParticipants().length);
        Map<String,String> participantNamesInCompareTo = Arrays.stream(compareTo.conversationParticipants()).collect(Collectors.toMap(ConversationParticipant::firstName, ConversationParticipant::lastName));
        for(int i = 0; i < participantFirstNames.length; i++) {
            var participantLastNameInCompareTo = participantNamesInCompareTo.get(participantFirstNames[i]);
            assert(participantLastNameInCompareTo != null);
            assert(participantLastNameInCompareTo.equals(participantLastNames[i]));
        }

        assert(messages.length == compareTo.messages().length);
        var messagesTextSet = Arrays.stream(compareTo.messages()).map(Message::text).collect(Collectors.toSet());
        for(var message : messages) {
            assert(messagesTextSet.contains(message));
        }

        assert(draftedMessage.equals(compareTo.draftedMessage()));
    }

    private static void compareMessages(Message[] messagesA, Message[] messagesB) {

        assert(messagesA.length == messagesB.length);

        Comparator<Message> comparator = new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o1.messageID().compareTo(o2.messageID());
            }
        };

        var listA = new ArrayList<>(Arrays.stream(messagesA).toList());
        listA.sort(comparator);
        var listB = new ArrayList<>(Arrays.stream(messagesB).toList());
        listB.sort(comparator);

        for(int i = 0; i < listA.size(); i++) {
            var mA = listA.get(i);
            var mB = listB.get(i);
            assert(mA.text() == null ? mB.text() == null : mA.text().equals(mB.text()));
            assert(mA.messageID().equals(mB.messageID()));
            assert(mA.timeSent() == mB.timeSent());
            assert(mA.participantId().equals(mB.participantId()));
        }
    }



    private static Conversation convertConversationServerOnlyToConversation(ConversationServerOnly conversation) {
        var participants = Arrays.stream(conversation.conversationParticipants()).map(cp ->
                new ConversationParticipant(cp.firstName(), cp.lastName(), cp.participantId())).toList().toArray(new ConversationParticipant[0]);
        return new Conversation(conversation.id(), conversation.dateStarted(), participants, conversation.messages(), conversation.draftedMessage(), conversation.name());
    }
}
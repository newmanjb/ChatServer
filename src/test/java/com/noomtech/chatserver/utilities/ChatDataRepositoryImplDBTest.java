package com.noomtech.chatserver.utilities;

import com.noomtech.chatserver.model.conversation.*;
import com.noomtech.chatserver.model.user.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.stream.Collectors;

public class ChatDataRepositoryImplDBTest {


    private static ChatDataRepository CHAT_DATA_REPOSITORY_IMPL_DB;
    private static Connection CONNECTION;


    @BeforeAll
    public static void beforeTest() throws Exception {
        System.setProperty("DB_CONFIG", "jdbc:postgresql://localhost:5432/,postgres,postgres,80");
        CHAT_DATA_REPOSITORY_IMPL_DB = ChatDataRepositoryImplDB.getInstance();
        CONNECTION = DriverManager.getConnection("postgresql://localhost:5432", "postgres", "postgres");
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

        //Check that the login functionality functions properly
        assert(chatDataRepository.checkLogin("newmanjb", "plums") != null);
        assert(chatDataRepository.checkLogin("newmanee", "plums1") == null);
        assert(chatDataRepository.checkLogin("d43d3d", "plums") == null);

        //Create 2 new conversations.  One with all users.  One with just 2.
        //Will need to convert them from the server-only objects to the client side objects
        var conversationAdults = convertConversationServerOnlyToConversation(CHAT_DATA_REPOSITORY_IMPL_DB.addNewConversation("Everyone", new String[]{"Joshua", "Elizabeth", "Eve", "Beth"}, new String[]{"Newman", "Newman", "Newman", "Newman"}));
        var conversationEveryone = convertConversationServerOnlyToConversation(CHAT_DATA_REPOSITORY_IMPL_DB.addNewConversation("Adults", new String[]{"Joshua", "Elizabeth"}, new String[]{"Newman", "Newman"}));

        //Check that the details of the conversation objects returned are correct based on what we specified when creating them
        checkConversationDetails(conversationEveryone, "Everyone", new String[]{"Joshua", "Elizabeth", "Eve", "Beth"}, new String[]{"Newman", "Newman", "Newman", "Newman"}, new String[0], "");
        checkConversationDetails(conversationAdults, "Adults", new String[]{"Joshua", "Elizabeth"}, new String[]{"Newman", "Newman"}, new String[0], "");

        //Get the participant ids for the users in each conversation.  We'll need these.
        var participantIdsMapEveryone = Arrays.stream(conversationAdults.conversationParticipants()).collect(Collectors.toMap(ConversationParticipant::firstName, ConversationParticipant::participantId));
        var participantIdsMapAdults = Arrays.stream(conversationEveryone.conversationParticipants()).collect(Collectors.toMap(ConversationParticipant::firstName, ConversationParticipant::participantId));
        var partIdsArrayEveryone = participantIdsMapEveryone.values().toArray(new UUID[0]);
        var partIdsArrayAdults = participantIdsMapAdults.values().toArray(new UUID[0]);

        //Check that all the participant ids are unique across both conversations
        checkAllAreDifferentInArray(partIdsArrayEveryone);
        checkAllAreDifferentInArray(partIdsArrayAdults);
        checkNoneAreTheSame(partIdsArrayEveryone, partIdsArrayAdults);

        //-------Check that the various methods to get the conversation details for a user function correctly---------------

        //Get user ids of participants - Use Eve's participant id - could just as easily user any of the other user's participant ids
        Set<UUID> userIdsOfParticipantsInConversationEveryone = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Eve"))).collect(Collectors.toSet());
        assert(userIdsOfParticipantsInConversationEveryone.size() == 4);
        assert(userIdsOfParticipantsInConversationEveryone.contains(josh.id()));
        assert(userIdsOfParticipantsInConversationEveryone.contains(liz.id()));
        assert(userIdsOfParticipantsInConversationEveryone.contains(eve.id()));
        assert(userIdsOfParticipantsInConversationEveryone.contains(beth.id()));
        Set<UUID> userIdsOfEveryoneInConversationAdults = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Josh"))).collect(Collectors.toSet());
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
        var message0 = new Message("Hi everyone", now - 100, participantIdsMapEveryone.get("Josh"));
        var message1 = new Message("Hi Josh", now - 80, participantIdsMapEveryone.get("Eve"));
        var message2 = new Message("Hello Josh", now - 60, participantIdsMapEveryone.get("Liz"));
        var message3 = new Message("Morning Josh", now - 40, participantIdsMapEveryone.get("Beth"));
        var message4 = new Message("How are we doing this morning?", now - 20, participantIdsMapEveryone.get("Josh"));
        var message5 = new Message("Eve - are you ready for school?", now - 10, participantIdsMapEveryone.get("Josh"));
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message0);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message1);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message2);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message3);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message4);
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message5);

        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(participantIdsMapEveryone.get("Beth")).conversations()[0].messages());

        //----------------------------------------------------------------------------------------------------------------------

        //-------Remove users from conversation with everyone------------------------------
        var message6= new Message("Yes.  Byeee", now - 9, participantIdsMapEveryone.get("Eve"));
        //The call should return false, as there are still users left in the conversation
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Eve")));
        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5, message6},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(participantIdsMapEveryone.get("Beth")).conversations()[0].messages());
        //Should only be 3 users in there now
        var userIds1 = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Beth"))).collect(Collectors.toSet());
        assert(userIds1.contains(participantIdsMapEveryone.get("Josh")));
        assert(userIds1.contains(participantIdsMapEveryone.get("Beth")));
        assert(userIds1.contains(participantIdsMapEveryone.get("Liz")));
        //Eve should not be a member of any conversations
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(eve.id()).conversations().length == 0);
        //The other users will still be part of it
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations().length == 2);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations().length == 2);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations().length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations()[0].name().equals("Everyone"));
        //This call should now only result in 3 users
        var set1 = Arrays.stream(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Liz"))).collect(Collectors.toSet());
        assert(set1.size() == 3);
        assert(set1.contains(josh.id()));
        assert(set1.contains(beth.id()));
        assert(set1.contains(liz.id()));

        //Remove 2 more users
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Josh")));
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Beth")));

        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations().length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations()[0].name().equals("Adults"));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(beth.id()).conversations().length == 0);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Liz")).length == 1) &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Liz"))[0].equals(liz.id());

        //Check that the messages are unaffected using the last user's participant id
        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5, message6},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(participantIdsMapEveryone.get("Liz")).conversations()[0].messages());

        //Finally, send a message from the last user and then remove them, which should result in the entire conversation being removed
        var message7 = new Message("Over and out", now - 8, participantIdsMapEveryone.get("Liz"));
        CHAT_DATA_REPOSITORY_IMPL_DB.addNewMessage(message7);
        compareMessages(
                new Message[]{message0, message1, message2, message3, message4, message5, message6, message7},
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(participantIdsMapEveryone.get("Liz")).conversations()[0].messages());

        //The call should return true this time
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapEveryone.get("Liz")));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations().length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations()[0].name().equals("Adults"));
        //Check that the entire conversation has been removed from the db
        checkConversationHasBeenRemoved(conversationEveryone.id(), partIdsArrayEveryone);
        //This should now return an empty array
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapEveryone.get("Liz")).length == 0);

        //-----------------------------------------------------------------

        //-------------Send some message to the "adults" conversation and remove participants------------------

        //Sanity check this conversation to make sure it's unaffected by the happenings above with the other conversation

        compareConversations(conversationAdults, CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(liz.id()).conversations()[0]);
        compareConversations(conversationAdults, CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations()[0]);

        //Send some messages
        var message11 = new Message("Hi Liz", now - 2, participantIdsMapAdults.get("Josh"));
        var message12 = new Message("Afternoon Josh", now - 1, participantIdsMapAdults.get("Liz"));

        compareMessages(new Message[]{message11, message12}, CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(participantIdsMapAdults.get("Josh")).conversations()[0].messages());

        //Remove both users
        assert(!CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapAdults.get("Josh")));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getConversationsForUser(josh.id()).conversations().length == 0);
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Liz")).length == 1 &&
                CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Liz"))[0].equals(liz.id()));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.removeParticipantFromConversation(participantIdsMapAdults.get("Liz")));
        assert(CHAT_DATA_REPOSITORY_IMPL_DB.getUserIdsInConversationForParticipantId(participantIdsMapAdults.get("Liz")).length == 0);
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
        var sql3 = "SELECT COUNT(participant_id) FROM messages WHERE participant_id IN (?,?,?,?)";

        try(var statement1 = CONNECTION.prepareStatement(sql1);
            var statement2 = CONNECTION.prepareStatement(sql2);
            var statement3 = CONNECTION.prepareStatement(sql3)) {

            statement1.setObject(1, conversationId);
            statement2.setObject(2, conversationId);
            for(int i = 0; i < participantIds.length; i++) {
                statement3.setObject(i+1, participantIds[i]);
            }

            try(var rs1 = statement1.executeQuery();
                var rs2 = statement2.executeQuery();
                var rs3 = statement3.executeQuery()) {
                rs1.next();
                assert(rs1.getInt(1) == 0);
                rs2.next();
                assert(rs2.getInt(1) == 0);
                rs3.next();
                assert(rs3.getInt(1) == 0);
            }
        }
    }

    private static void checkAllAreDifferentInArray(Object[] a) {

        for(int i = 0; i < a.length; i++) {
            var sourceObject = a[i];
            for(int j = i+1; j < a.length; j++) {
                assert(sourceObject.equals(a[j]));
            }
        }
    }

    private static void checkNoneAreTheSame(Object[] a, Object[] b) {
        for(int i = 0; i < a.length; i++) {
            for(int j = 0; j < b.length; j++) {
                assert(a[i].equals(b[i]));
            }
        }
    }

    private static void checkUserAgainstDB(User user) throws Exception {
        try(var statement = CONNECTION.createStatement();
            var rs = statement.executeQuery("SELECT id, first_name, last_name, age, user_name, password FROM users")) {

            assert(rs.next());
            assert((rs.getObject(1)).equals(user.id()));
            assert(rs.getString(2).equals(user.firstName()));
            assert(rs.getString(3).equals(user.lastName()));
            assert(rs.getInt(4) == user.age());
            assert(rs.getString(5).equals(user.username()));
        }
    }

    private static boolean compareConversations(Conversation conversation1, Conversation conversation2) throws Exception {

        return conversation2.dateStarted() == conversation1.dateStarted() &&
                Objects.equals(conversation2.id(), conversation1.id()) &&
                Objects.equals(conversation2.name(), conversation1.name()) &&
                Objects.deepEquals(conversation2.messages(), conversation1.messages()) &&
                Objects.equals(conversation2.draftedMessage(), conversation1.draftedMessage()) &&
                Objects.deepEquals(conversation2.conversationParticipants(), conversation1.conversationParticipants());
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
        Comparator<Message> comparator = new Comparator<Message>() {
            @Override
            public int compare(Message o1, Message o2) {
                return o1.timeSent() > o2.timeSent() ? 1 : (o1.timeSent() == o2.timeSent() ? 0 : -1);
            }
        };

        var listA = new ArrayList<>(Arrays.stream(messagesA).toList());
        listA.sort(comparator);
        var listB = new ArrayList<>(Arrays.stream(messagesB).toList());
        listB.sort(comparator);

        for(int i = 0; i < listA.size(); i++) {
            assert(listA.get(i)).equals(listB.get(i));
        }
    }



    private static Conversation convertConversationServerOnlyToConversation(ConversationServerOnly conversation) {
        var participants = (ConversationParticipant[]) Arrays.stream(conversation.conversationParticipants()).map(cp ->
                new ConversationParticipant(cp.firstName(), cp.lastName(), cp.participantId())).toArray();
        return new Conversation(conversation.id(), conversation.dateStarted(), participants, conversation.messages(), conversation.draftedMessage(), conversation.name());
    }
}

//
//
//User josh = new User(UUID.randomUUID(), "Joshua", "Newman", "newmanjb", 48);
//User liz = new User(UUID.randomUUID(), "Elizabeth", "Newman", "newmaneb", 47);
//User eve = new User(UUID.randomUUID(), "Eve", "Newman", "newmanee", 11);
//User beth = new User(UUID.randomUUID(), "Beth", "Newman", "newmanbb", 9);
//
//        System.out.println("-----------------------------------------------");
//        System.out.println("Joshua's user id: " + josh.id());
//        System.out.println("Elizabeth's user id: " + liz.id());
//        System.out.println("Eve's user id: " + eve.id());
//        System.out.println("Beth's user id: " + beth.id());
//        System.out.println("-----------------------------------------------");
//
//ChatDataRepository chatDataRepository = ChatDataRepositoryImplDB.getInstance();
//
//        chatDataRepository.addNewUser(josh, "bollocks");
//        chatDataRepository.addNewUser(liz, "bollocks");
//        chatDataRepository.addNewUser(eve, "bollocks");
//        chatDataRepository.addNewUser(beth, "bollocks");
//
//        System.out.println("Login for Joshua is: " + chatDataRepository.checkLogin("newmanjb", "bollocks"));
//        System.out.println("Login for Eve is: " + chatDataRepository.checkLogin("newmanee", "bollocks1"));
//        System.out.println("Login for Beth is: " + chatDataRepository.checkLogin("d43d3d", "bollocks"));
//
//var conversation1 = chatDataRepository.addNewConversation("Everyone", new String[]{"Joshua", "Elizabeth", "Eve", "Beth"}, new String[]{"Newman", "Newman", "Newman", "Newman"});
//var conversation2 = chatDataRepository.addNewConversation("Adults", new String[]{"Joshua", "Elizabeth"}, new String[]{"Newman", "Newman"});
//
////Check that each participant id is different
//var participantIdsConv1 = Arrays.stream(conversation1.conversationParticipants()).collect(Collectors.toMap(ConversationParticipantServerOnly::firstName, ConversationParticipantServerOnly::participantId));
//var participantIdsConv2 = Arrays.stream(conversation2.conversationParticipants()).collect(Collectors.toMap(ConversationParticipantServerOnly::firstName, ConversationParticipantServerOnly::participantId));
//
////get user ids for each person.  Check they're all different.  Check the names and pwds and ages etc.. all match
//
////compare above user ids to user ids in these conversations and do assertions.  Also check conversation details match with what you've put in
//
//        System.out.println("User ids in Joshua's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Joshua"))));
//        System.out.println("User ids in Joshua's Conversation2\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv2.get("Joshua"))));
//        System.out.println("User ids in Eve's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Eve"))));
//        System.out.println("Conversations for Joshua\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("Conversations for Eve\n" + chatDataRepository.getConversationsForUser(eve.id()));
//        System.out.println("-----------------------------------------------");
//
//sendMessage("Hi everyone!", participantIdsConv1.get("Joshua"), chatDataRepository);
//
//sendMessage("Morning everyone!", participantIdsConv2.get("Elizabeth"), chatDataRepository);
//
//sendMessage("Hi Josh", participantIdsConv1.get("Beth"), chatDataRepository);
//
//sendMessage("Morning Liz!", participantIdsConv2.get("Joshua"), chatDataRepository);
//
////Conversation 1-------------
//sendMessage("Daddy where's my shampoo?", participantIdsConv1.get("Eve"), chatDataRepository);
//
//sendMessage("In the main bathroom", participantIdsConv1.get("Elizabeth"), chatDataRepository);
//
//sendMessage("OK going to school.  Bye, love you", participantIdsConv1.get("Beth"), chatDataRepository);
//
//        //Check messages in conversations.
//
//        System.out.println("User ids in Joshua's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Joshua"))));
//        System.out.println("User ids in Joshua's Conversation2\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv2.get("Joshua"))));
//        System.out.println("User ids in Eve's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Eve"))));
//        System.out.println("Conversations for Joshua\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("Conversations for Eve\n" + chatDataRepository.getConversationsForUser(eve.id()));
//        System.out.println("-----------------------------------------------");
//
//
//        chatDataRepository.removeParticipantFromConversation(participantIdsConv1.get("Beth"));
//
//        //check participants in conversations
//
//        System.out.println("User ids in Joshua's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Joshua"))));
//        System.out.println("User ids in Joshua's Conversation2\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv2.get("Joshua"))));
//        System.out.println("User ids in Eve's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Eve"))));
//        System.out.println("Conversations for Joshua\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("Conversations for Eve\n" + chatDataRepository.getConversationsForUser(eve.id()));
//        System.out.println("-----------------------------------------------");
//
//sendMessage("OK going to school.  Bye, love you", participantIdsConv1.get("Eve"), chatDataRepository);
//        chatDataRepository.removeParticipantFromConversation(participantIdsConv1.get("Eve"));
//        chatDataRepository.removeParticipantFromConversation(participantIdsConv1.get("Elizabeth"));
//
//        //check participants in conversations
//
//        System.out.println("User ids in Joshua's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Joshua"))));
//        System.out.println("User ids in Joshua's Conversation2\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv2.get("Joshua"))));
//        System.out.println("User ids in Eve's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Eve"))));
//        System.out.println("Conversations for Joshua\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("Conversations for Eve\n" + chatDataRepository.getConversationsForUser(eve.id()));
//        System.out.println("-----------------------------------------------");
//
//        chatDataRepository.removeParticipantFromConversation(participantIdsConv1.get("Joshua"));
//
//        //check participants in conversation and check all details of conversation 1, including messages, as in the earlier step above to ensure nothing about that has changed
//
//        System.out.println("User ids in Joshua's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Joshua"))));
//        System.out.println("User ids in Joshua's Conversation2\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv2.get("Joshua"))));
//        System.out.println("User ids in Eve's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Eve"))));
//        System.out.println("Conversations for Joshua\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("Conversations for Eve\n" + chatDataRepository.getConversationsForUser(eve.id()));
//        System.out.println("-----------------------------------------------");
////---------------------------
//
////Conversation 2-------------
//sendMessage("Where are the swimming trunks?", participantIdsConv2.get("Elizabeth"), chatDataRepository);
//sendMessage("I thought they were in Eve's bedroom", participantIdsConv2.get("Elizabeth"), chatDataRepository);
//sendMessage("That's where they should be", participantIdsConv2.get("Joshua"), chatDataRepository);
//sendMessage("If not they'll be in the utility room", participantIdsConv2.get("Joshua"), chatDataRepository);
//
//        //check messages in conversation
//
//        System.out.println("User ids in Joshua's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Joshua"))));
//        System.out.println("User ids in Joshua's Conversation2\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv2.get("Joshua"))));
//        System.out.println("User ids in Eve's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Eve"))));
//        System.out.println("Conversations for Joshua\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("Conversations for Eve\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("-----------------------------------------------");
//
//        chatDataRepository.removeParticipantFromConversation(participantIdsConv2.get("Elizabeth"));
//
//        //check participants in conversation
//
//        System.out.println("User ids in Joshua's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Joshua"))));
//        System.out.println("User ids in Joshua's Conversation2\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv2.get("Joshua"))));
//        System.out.println("User ids in Eve's Conversation1\n" + Arrays.toString(chatDataRepository.getUserIdsInConversationForParticipantId(participantIdsConv1.get("Eve"))));
//        System.out.println("Conversations for Joshua\n" + chatDataRepository.getConversationsForUser(josh.id()));
//        System.out.println("Conversations for Eve\n" + chatDataRepository.getConversationsForUser(eve.id()));
//        System.out.println("-----------------------------------------------");
//
//
//        chatDataRepository.removeParticipantFromConversation(participantIdsConv2.get("Joshua"));
//
////check participants in conversation and check all details of conversation 2, including messages, as in the earlier step above to ensure nothing about that has changed
//
////---------------------------

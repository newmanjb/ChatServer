create table users (
user_id uuid, age integer not null, first_name varchar(40) not null, last_name varchar(50) not null, primary key(user_id)
);

create table conversations (conversation_id uuid, conversation_name varchar(50) not null, date_started timestamp not null, date_ended timestamp, primary key (conversation_id));

create table drafted_messages (user_id uuid, conversation_id uuid, msg_text text, primary key (user_id, conversation_id), constraint fk_drafted_messages_users foreign key(user_id) references users(user_id), constraint fk_drafted_messages_conversations foreign key(conversation_id) references conversations(conversation_id));

create table conversation_participants(conversation_id uuid, user_id uuid, primary key (conversation_id, user_id), constraint fk_conversation_participants_conversations foreign key(conversation_id) references conversations(conversation_id), constraint fk_conversation_participants_users foreign key(user_id) references users(user_id));

create table messages(message_id uuid, date_sent timestamp, sent_from uuid, msg_text text, primary key(message_id), constraint fk_messages_users foreign key(sent_from) references users(user_id));

create table conversation_messages(conversation_id uuid, message_id uuid, primary key (conversation_id, message_id), constraint fk_conversation_messages_conversations foreign key(conversation_id) references conversations(conversation_id), constraint fk_conversation_messages_messages foreign key(message_id) references messages(message_id));

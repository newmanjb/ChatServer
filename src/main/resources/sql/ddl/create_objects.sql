drop table if exists conversations cascade;
drop table if exists conversation_participants cascade;
drop table if exists messages cascade;
drop table if exists users cascade;
drop table if exists drafted_messages cascade;

create table users (
user_id uuid, age integer not null, first_name varchar(40) not null, last_name varchar(50) not null, primary key(user_id)
);

create table conversations (conversation_id uuid, conversation_name varchar(50) not null, date_started timestamp not null, date_ended timestamp, primary key (conversation_id));

create table conversation_participants(conversation_id uuid, user_id uuid, participant_id uuid unique not null, primary key (conversation_id, user_id), constraint fk_conversation_participants_users foreign key(user_id) references users(user_id), constraint fk_conversation_participants_conversations foreign key(conversation_id) references conversations(conversation_id));

create table drafted_messages (participant_id uuid, msg_text text not null, primary key (participant_id), constraint fk_drafted_messages_conversation_participants foreign key(participant_id) references conversation_participants(participant_id));


create table messages(message_id uuid, date_sent timestamp not null, participant_id uuid not null, msg_text text not null, primary key(message_id), constraint fk_messages_conversation_participants foreign key(participant_id) references conversation_participants(participant_id));




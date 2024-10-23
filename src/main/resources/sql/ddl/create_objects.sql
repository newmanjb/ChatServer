drop table if exists conversations cascade;
drop table if exists conversation_participants cascade;
drop table if exists messages cascade;
drop table if exists users cascade;
drop table if exists drafted_messages cascade;


-- Table: public.users

-- DROP TABLE IF EXISTS public.users;

CREATE TABLE IF NOT EXISTS public.users
(
    user_id uuid NOT NULL,
    age integer NOT NULL,
    first_name character varying(40) COLLATE pg_catalog."default" NOT NULL,
    last_name character varying(50) COLLATE pg_catalog."default" NOT NULL,
    username character varying(10) COLLATE pg_catalog."default" NOT NULL,
    password character varying(20) COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT users_pkey PRIMARY KEY (user_id),
    UNIQUE (first_name, last_name)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.users
    OWNER to postgres;



-- Table: public.conversations

-- DROP TABLE IF EXISTS public.conversations;

CREATE TABLE IF NOT EXISTS public.conversations
(
    conversation_id uuid NOT NULL,
    conversation_name character varying(50) COLLATE pg_catalog."default" NOT NULL,
    date_started timestamp without time zone NOT NULL,
    date_ended timestamp without time zone,
    CONSTRAINT conversations_pkey PRIMARY KEY (conversation_id)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.conversations
    OWNER to postgres;




-- Table: public.conversation_participants

-- DROP TABLE IF EXISTS public.conversation_participants;

CREATE TABLE IF NOT EXISTS public.conversation_participants
(
    conversation_id uuid NOT NULL,
    user_id uuid NOT NULL,
    participant_id uuid NOT NULL,
    CONSTRAINT conversation_participants_pkey PRIMARY KEY (conversation_id, user_id),
    CONSTRAINT conversation_participants_participant_id_key UNIQUE (participant_id),
    CONSTRAINT fk_conversation_participants_conversations FOREIGN KEY (conversation_id)
        REFERENCES public.conversations (conversation_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.conversation_participants
    OWNER to postgres;



-- Table: public.messages

-- DROP TABLE IF EXISTS public.messages;

CREATE TABLE IF NOT EXISTS public.messages
(
    message_id uuid NOT NULL,
    date_sent timestamp without time zone NOT NULL,
    participant_id uuid NOT NULL,
    msg_text text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT messages_pkey PRIMARY KEY (message_id),
    CONSTRAINT fk_messages_conversation_participants FOREIGN KEY (participant_id)
        REFERENCES public.conversation_participants (participant_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.messages
    OWNER to postgres;





-- Table: public.drafted_messages

-- DROP TABLE IF EXISTS public.drafted_messages;

CREATE TABLE IF NOT EXISTS public.drafted_messages
(
    participant_id uuid NOT NULL,
    msg_text text COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT drafted_messages_pkey PRIMARY KEY (participant_id),
    CONSTRAINT fk_drafted_messages_conversation_participants FOREIGN KEY (participant_id)
        REFERENCES public.conversation_participants (participant_id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.drafted_messages
    OWNER to postgres;
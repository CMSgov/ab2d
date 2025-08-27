CREATE SEQUENCE IF NOT EXISTS event.event_api_request_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS event.event_api_response_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS event.event_bene_reload_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS event.event_error_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS event.event_file_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS event.event_job_status_change_id_seq START 1;
CREATE SEQUENCE IF NOT EXISTS event.event_bene_search_id_seq START 1;

CREATE TABLE IF NOT EXISTS event.event_api_request (
    id integer NOT NULL DEFAULT nextval('event.event_api_request_id_seq'::regclass),
    time_of_event timestamp with time zone,
                                job_id character varying(255) COLLATE pg_catalog."default",
    url character varying(2048) COLLATE pg_catalog."default",
    ip_address character varying(32) COLLATE pg_catalog."default",
    token_hash character varying(255) COLLATE pg_catalog."default",
    request_id character varying(255) COLLATE pg_catalog."default",
    aws_id character varying(255) COLLATE pg_catalog."default",
    environment character varying(255) COLLATE pg_catalog."default",
    organization character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT event_api_request_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event.event_api_response (
    id integer NOT NULL DEFAULT nextval('event.event_api_response_id_seq'::regclass),
    time_of_event timestamp with time zone,
                                job_id character varying(255) COLLATE pg_catalog."default",
    response_code integer,
    response_string text COLLATE pg_catalog."default",
    description text COLLATE pg_catalog."default",
    request_id character varying(255) COLLATE pg_catalog."default",
    aws_id character varying(255) COLLATE pg_catalog."default",
    environment character varying(255) COLLATE pg_catalog."default",
    organization character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT event_api_response_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event.event_bene_reload (
    id integer NOT NULL DEFAULT nextval('event.event_bene_reload_id_seq'::regclass),
    time_of_event timestamp with time zone,
                                job_id character varying(255) COLLATE pg_catalog."default",
    file_type character varying(255) COLLATE pg_catalog."default",
    file_name character varying(500) COLLATE pg_catalog."default",
    number_loaded integer,
    aws_id character varying(255) COLLATE pg_catalog."default",
    environment character varying(255) COLLATE pg_catalog."default",
    organization character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT event_bene_reload_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event.event_bene_search (
    id integer NOT NULL DEFAULT nextval('event.event_bene_search_id_seq'::regclass),
    time_of_event timestamp with time zone,
                                job_id character varying(255) COLLATE pg_catalog."default",
    contract_number character varying(255) COLLATE pg_catalog."default",
    benes_expected integer,
    benes_searched integer,
    num_opted_out integer,
    benes_errored integer,
    aws_id character varying(255) COLLATE pg_catalog."default",
    environment character varying(255) COLLATE pg_catalog."default",
    organization character varying(255) COLLATE pg_catalog."default",
    benes_queued integer DEFAULT 0,
    eobs_fetched integer DEFAULT 0,
    eobs_written integer DEFAULT 0,
    eob_files integer DEFAULT 0,
    benes_with_eobs integer DEFAULT 0,
    CONSTRAINT event_bene_search_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event.event_error (
    id integer NOT NULL DEFAULT nextval('event.event_error_id_seq'::regclass),
    time_of_event timestamp with time zone,
                                job_id character varying(255) COLLATE pg_catalog."default",
    error_type character varying(255) COLLATE pg_catalog."default",
    description text COLLATE pg_catalog."default",
    aws_id character varying(255) COLLATE pg_catalog."default",
    environment character varying(255) COLLATE pg_catalog."default",
    organization character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT event_error_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event.event_file (
    id integer NOT NULL DEFAULT nextval('event.event_file_id_seq'::regclass),
    time_of_event timestamp with time zone,
                                job_id character varying(255) COLLATE pg_catalog."default",
    file_name character varying(500) COLLATE pg_catalog."default",
    status character varying(255) COLLATE pg_catalog."default",
    file_size bigint,
    file_hash character varying(255) COLLATE pg_catalog."default",
    aws_id character varying(255) COLLATE pg_catalog."default",
    environment character varying(255) COLLATE pg_catalog."default",
    organization character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT event_file_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event.event_job_status_change (
    id integer NOT NULL DEFAULT nextval('event.event_job_status_change_id_seq'::regclass),
    time_of_event timestamp with time zone,
                                job_id character varying(255) COLLATE pg_catalog."default",
    old_status character varying(255) COLLATE pg_catalog."default",
    new_status character varying(255) COLLATE pg_catalog."default",
    description text COLLATE pg_catalog."default",
    aws_id character varying(255) COLLATE pg_catalog."default",
    environment character varying(255) COLLATE pg_catalog."default",
    organization character varying(255) COLLATE pg_catalog."default",
    CONSTRAINT event_job_status_change_pkey PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event.event_metrics
(
    id            BIGSERIAL PRIMARY KEY,
    service       varchar(64) not null COLLATE pg_catalog."default",
    state_type       varchar(64) not null COLLATE pg_catalog."default", -- is the event beginning, continuing, or ending
    event_description varchar(4096) COLLATE pg_catalog."default",
    time_of_event timestamp with time zone,
    awsId         varchar(255) COLLATE pg_catalog."default",
    environment   varchar(255) COLLATE pg_catalog."default",
    job_id        varchar(255) COLLATE pg_catalog."default"
);

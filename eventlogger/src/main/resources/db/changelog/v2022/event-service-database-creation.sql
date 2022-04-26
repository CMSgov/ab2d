------------------------------
-- Create ab2d_analyst user
------------------------------

CREATE SCHEMA IF NOT EXISTS event;


DO
$$
    begin
        if not exists(SELECT * FROM pg_user WHERE usename = 'ab2d_analyst') THEN
            Create Role ab2d_analyst noinherit login password 'ab2d';
        end if;

        if not exists(SELECT * FROM pg_user WHERE usename = 'cmsadmin') THEN
            --- login for cms admin
            Create Role cmsadmin noinherit login password 'ab2d';
        end if;
    end
$$;

CREATE SCHEMA IF NOT EXISTS event;
-- Table: event_api_request
-- DROP TABLE IF EXISTS event_api_request;
CREATE TABLE IF NOT EXISTS event.event_api_request
(
    id SERIAL,
    time_of_event timestamp with time zone,
    job_id character varying(255),
    url character varying(2048),
    ip_address character varying(32),
    token_hash character varying(255),
    request_id character varying(255),
    aws_id character varying(255),
    environment character varying(255),
    organization character varying(255),
    CONSTRAINT pk_api_request_event PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS event.event_api_request
    OWNER to cmsadmin;
GRANT SELECT ON TABLE event.event_api_request TO ab2d_analyst;
GRANT ALL ON TABLE event.event_api_request TO cmsadmin;



-- Table: event_api_response
-- DROP TABLE IF EXISTS event_api_response
CREATE TABLE IF NOT EXISTS event.event_api_response
(
    id SERIAL,
    time_of_event timestamp with time zone,
    job_id character varying(255),
    response_code integer,
    response_string text,
    description text,
    request_id character varying(255),
    aws_id character varying(255),
    environment character varying(255),
    organization character varying(255),
    CONSTRAINT pk_api_response_event PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS event.event_api_response
    OWNER to cmsadmin;
GRANT SELECT ON TABLE event.event_api_response TO ab2d_analyst;
GRANT ALL ON TABLE event.event_api_response TO cmsadmin;




-- Table: event_bene_coverage_search_status_change
-- DROP TABLE IF EXISTS event_bene_coverage_search_status_change;
CREATE TABLE IF NOT EXISTS event.event_bene_coverage_search_status_change
(
    id SERIAL,
    bene_coverage_period_id integer NOT NULL,
    old_status character varying(255),
    new_status character varying(255) NOT NULL,
    created timestamp with time zone,
    modified timestamp with time zone,
    description text,
    CONSTRAINT pk_event_bene_coverage_search PRIMARY KEY (id));

ALTER TABLE IF EXISTS event.event_bene_coverage_search_status_change
    OWNER to cmsadmin;
GRANT SELECT ON TABLE event.event_bene_coverage_search_status_change TO ab2d_analyst;
GRANT ALL ON TABLE event.event_bene_coverage_search_status_change TO cmsadmin;

-- Index: ix_bene_coverage_search_status_job_id
-- DROP INDEX IF EXISTS ix_bene_coverage_search_status_job_id;
CREATE INDEX IF NOT EXISTS ix_bene_coverage_search_status_job_id
    ON event.event_bene_coverage_search_status_change USING btree
        (bene_coverage_period_id ASC NULLS LAST);



-- Table: event_bene_reload
-- DROP TABLE IF EXISTS event_bene_reload;
CREATE TABLE IF NOT EXISTS event.event_bene_reload
(
    id SERIAL,
    time_of_event timestamp with time zone,
    job_id character varying(255),
    file_type character varying(255),
    file_name character varying(500),
    number_loaded integer,
    aws_id character varying(255),
    environment character varying(255),
    organization character varying(255),
    CONSTRAINT pk_bene_reload_event PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS event.event_bene_reload
    OWNER to cmsadmin;
GRANT SELECT ON TABLE event.event_bene_reload TO ab2d_analyst;
GRANT ALL ON TABLE event.event_bene_reload TO cmsadmin;





-- Table: event_bene_search
-- DROP TABLE IF EXISTS event_bene_search;
CREATE TABLE IF NOT EXISTS event.event_bene_search
(
    id SERIAL,
    time_of_event timestamp with time zone,
    job_id character varying(255),
    contract_number character varying(255),
    benes_expected integer,
    benes_searched integer,
    num_opted_out integer,
    benes_errored integer,
    aws_id character varying(255),
    environment character varying(255),
    organization character varying(255),
    benes_queued integer DEFAULT 0,
    eobs_fetched integer DEFAULT 0,
    eobs_written integer DEFAULT 0,
    eob_files integer DEFAULT 0,
    benes_with_eobs integer DEFAULT 0,
    CONSTRAINT pk_bene_search_event PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS event.event_bene_search
    OWNER to cmsadmin;
GRANT SELECT ON TABLE event.event_bene_search TO ab2d_analyst;
GRANT ALL ON TABLE event.event_bene_search TO cmsadmin;
-- Index: ix_bene_search_job_id
-- DROP INDEX IF EXISTS ix_bene_search_job_id;

CREATE INDEX IF NOT EXISTS ix_bene_search_job_id
    ON event.event_bene_search USING btree
        (job_id ASC NULLS LAST);



-- Table: event_error
-- DROP TABLE IF EXISTS event_error;
CREATE TABLE IF NOT EXISTS event.event_error
(
    id SERIAL,
    time_of_event timestamp with time zone,
    job_id character varying(255),
    error_type character varying(255),
    description text,
    aws_id character varying(255),
    environment character varying(255),
    organization character varying(255),
    CONSTRAINT pk_error_event PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS event.event_error
    OWNER to cmsadmin;
GRANT SELECT ON TABLE event.event_error TO ab2d_analyst;
GRANT ALL ON TABLE event.event_error TO cmsadmin;




-- Table: event_file
-- DROP TABLE IF EXISTS event_file;
CREATE TABLE IF NOT EXISTS event.event_file
(
    id SERIAL,
    time_of_event timestamp with time zone,
    job_id character varying(255),
    file_name character varying(500),
    status character varying(255),
    file_size bigint,
    file_hash character varying(255),
    aws_id character varying(255),
    environment character varying(255),
    organization character varying(255),
    CONSTRAINT pk_file_event PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS event.event_file
    OWNER to cmsadmin;
GRANT SELECT ON TABLE event.event_file TO ab2d_analyst;
GRANT ALL ON TABLE event.event_file TO cmsadmin;

-- Index: ix_file_event_job_id
-- DROP INDEX IF EXISTS ix_file_event_job_id;
CREATE INDEX IF NOT EXISTS ix_file_event_job_id
    ON event.event_file USING btree
        (job_id ASC NULLS LAST);





-- Table: event_job_status_change
-- DROP TABLE IF EXISTS event_job_status_change;
CREATE TABLE IF NOT EXISTS event.event_job_status_change
(
    id SERIAL,
    time_of_event timestamp with time zone,
    job_id character varying(255),
    old_status character varying(255),
    new_status character varying(255),
    description text,
    aws_id character varying(255),
    environment character varying(255),
    organization character varying(255),
    CONSTRAINT pk_job_status_change_event PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS event.event_job_status_change
    OWNER to cmsadmin;

GRANT SELECT ON TABLE event.event_job_status_change TO ab2d_analyst;

GRANT ALL ON TABLE event.event_job_status_change TO cmsadmin;
-- Index: ix_job_status_job_id
-- DROP INDEX IF EXISTS ix_job_status_job_id;
CREATE INDEX IF NOT EXISTS ix_job_status_job_id
    ON event.event_job_status_change USING btree
        (job_id ASC NULLS LAST);

GRANT SELECT ON event.event_api_response TO ab2d_analyst;
GRANT SELECT ON event.event_api_request TO ab2d_analyst;
GRANT SELECT ON event.event_bene_coverage_search_status_change TO ab2d_analyst;
GRANT SELECT ON event.event_bene_reload TO ab2d_analyst;
GRANT SELECT ON event.event_bene_search TO ab2d_analyst;
GRANT SELECT ON event.event_error TO ab2d_analyst;
GRANT SELECT ON event.event_file TO ab2d_analyst;
GRANT SELECT ON event.event_job_status_change TO ab2d_analyst;
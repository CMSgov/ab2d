--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-1242-persist-events-api-request failOnError:true

CREATE TABLE event_api_request
(
        id SERIAL,
        time_of_event TIMESTAMP WITH TIME ZONE,
        job_id VARCHAR(255),
        user_id VARCHAR(255),
        url VARCHAR(2048),
        ip_address VARCHAR(32),
        token_hash VARCHAR(32),
        request_id VARCHAR(32)
);
ALTER TABLE event_api_request ADD CONSTRAINT "pk_api_request_event" PRIMARY KEY (id);
CREATE INDEX "ix_api_request_user" ON event_api_request (user_id);

--rollback DROP TABLE event_api_request;
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-1242-persist-events-api-response failOnError:true

CREATE TABLE event_api_response
(
        id SERIAL,
        time_of_event TIMESTAMP WITH TIME ZONE,
        user_id VARCHAR(255),
        job_id VARCHAR(255),
        response_code INT,
        response_string TEXT,
        description TEXT,
        request_id VARCHAR (32)
);
ALTER TABLE event_api_response ADD CONSTRAINT "pk_api_response_event" PRIMARY KEY (id);
CREATE INDEX "ix_api_response_user" ON event_api_response (user_id);

--rollback DROP TABLE event_api_response;
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-1242-persist-events-bene-reload failOnError:true

CREATE TABLE event_bene_reload
(
        id SERIAL,
        time_of_event TIMESTAMP WITH TIME ZONE,
        user_id VARCHAR(255),
        job_id VARCHAR(255),
        file_type VARCHAR (255),
        file_name VARCHAR (500),
        number_loaded INT
);
ALTER TABLE event_bene_reload ADD CONSTRAINT "pk_bene_reload_event" PRIMARY KEY (id);

--rollback DROP TABLE event_bene_reload;
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-1242-persist-events-bene-search failOnError:true

CREATE TABLE event_bene_search
(
        id SERIAL,
        time_of_event TIMESTAMP WITH TIME ZONE,
        user_id VARCHAR(255),
        job_id VARCHAR(255),
        contract_number VARCHAR(255),
        num_in_contract INT,
        num_searched INT,
        num_opted_out INT,
        num_errors INT
);
ALTER TABLE event_bene_search ADD CONSTRAINT "pk_bene_search_event" PRIMARY KEY (id);
CREATE INDEX "ix_bene_search_job_id" ON event_bene_search (job_id);

--rollback DROP TABLE bene_search;
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-1242-persist-events-error failOnError:true

CREATE TABLE event_error
(
        id SERIAL,
        time_of_event TIMESTAMP WITH TIME ZONE,
        user_id VARCHAR(255),
        job_id VARCHAR(255),
        error_type VARCHAR(255),
        description TEXT
);
ALTER TABLE event_error ADD CONSTRAINT "pk_error_event" PRIMARY KEY (id);

--rollback DROP TABLE event_error;
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-1242-persist-events-file failOnError:true

CREATE TABLE event_file
(
        id SERIAL,
        time_of_event TIMESTAMP WITH TIME ZONE,
        user_id VARCHAR(255),
        job_id VARCHAR(255),
        file_name VARCHAR (500),
        status VARCHAR (255),
        file_size BIGINT,
        file_hash VARCHAR (255)
);
ALTER TABLE event_file ADD CONSTRAINT "pk_file_event" PRIMARY KEY (id);
CREATE INDEX "ix_file_event_job_id" ON event_file (job_id);

--rollback DROP TABLE event_file
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-1242-persist-events-job_status_change failOnError:true

CREATE TABLE event_job_status_change
(
        id SERIAL,
        time_of_event TIMESTAMP WITH TIME ZONE,
        user_id VARCHAR(255),
        job_id VARCHAR(255),
        old_status VARCHAR (255),
        new_status VARCHAR (255),
        description TEXT
);
ALTER TABLE event_job_status_change ADD CONSTRAINT "pk_job_status_change_event" PRIMARY KEY (id);
CREATE INDEX "ix_job_status_job_id" ON event_job_status_change (job_id);

--rollback DROP TABLE event_job_status_change;
--  -------------------------------------------------------------------------------------------------------------------


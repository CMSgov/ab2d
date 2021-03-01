--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:rework_event_tables failOnError:true

-- Remove okta client ids from other database
-- event_api_request
-- event_api_response
-- event_bene_reload
-- event_error
-- event_file
-- event_job_status_change

-- event_api_request table
ALTER TABLE event_api_request ADD COLUMN organization VARCHAR(255);

UPDATE event_api_request
SET organization = user_account.organization
FROM user_account
WHERE event_api_request.user_id = user_account.client_id;

ALTER TABLE event_api_request DROP COLUMN user_id;

-- event_api_response table
ALTER TABLE event_api_response ADD COLUMN organization VARCHAR(255);

UPDATE event_api_response
SET organization = user_account.organization
FROM user_account
WHERE event_api_response.user_id = user_account.client_id;

ALTER TABLE event_api_response DROP COLUMN user_id;

-- event_bene_reload
ALTER TABLE event_bene_reload ADD COLUMN organization VARCHAR(255);

UPDATE event_bene_reload
SET organization = user_account.organization
FROM user_account
WHERE event_bene_reload.user_id = user_account.client_id;

ALTER TABLE event_bene_reload DROP COLUMN user_id;

-- event_bene_search
ALTER TABLE event_bene_search ADD COLUMN organization VARCHAR(255);

UPDATE event_bene_search
SET organization = user_account.organization
FROM user_account
WHERE event_bene_search.user_id = user_account.client_id;

ALTER TABLE event_bene_search DROP COLUMN user_id;

-- event_error
ALTER TABLE event_error ADD COLUMN organization VARCHAR(255);

UPDATE event_error
SET organization = user_account.organization
FROM user_account
WHERE event_error.user_id = user_account.client_id;

ALTER TABLE event_error DROP COLUMN user_id;

-- event_file
ALTER TABLE event_file ADD COLUMN organization VARCHAR(255);

UPDATE event_file
SET organization = user_account.organization
FROM user_account
WHERE event_file.user_id = user_account.client_id;

ALTER TABLE event_file DROP COLUMN user_id;

-- event_job_status_change
ALTER TABLE event_job_status_change ADD COLUMN organization VARCHAR(255);

UPDATE event_job_status_change
SET organization = user_account.organization
FROM user_account
WHERE event_job_status_change.user_id = user_account.client_id;

ALTER TABLE event_job_status_change DROP COLUMN user_id;
--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:ab2d-1377-create-kinesis-logger failOnError:true
ALTER TABLE event_api_request ADD COLUMN environment VARCHAR(255);
ALTER TABLE event_api_response ADD COLUMN environment VARCHAR(255);
ALTER TABLE event_bene_reload ADD COLUMN environment VARCHAR(255);
ALTER TABLE event_bene_search ADD COLUMN environment VARCHAR(255);
ALTER TABLE event_error ADD COLUMN environment VARCHAR(255);
ALTER TABLE event_file ADD COLUMN environment VARCHAR(255);
ALTER TABLE event_job_status_change ADD COLUMN environment VARCHAR(255);

--rollback  ALTER TABLE event_api_request DROP COLUMN environment;
--rollback  ALTER TABLE event_api_response DROP COLUMN environment;
--rollback  ALTER TABLE event_bene_reload DROP COLUMN environment;
--rollback  ALTER TABLE event_bene_search DROP COLUMN environment;
--rollback  ALTER TABLE event_error ADD DROP environment;
--rollback  ALTER TABLE event_file ADD DROP environment;
--rollback  ALTER TABLE event_job_status_change DROP COLUMN environment;

--  -------------------------------------------------------------------------------------------------------------------



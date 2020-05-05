--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:ab2d-1377-create-kinesis-logger failOnError:true
ALTER TABLE event_api_request ADD COLUMN aws_id VARCHAR(255);
ALTER TABLE event_api_response ADD COLUMN aws_id VARCHAR(255);
ALTER TABLE event_bene_reload ADD COLUMN aws_id VARCHAR(255);
ALTER TABLE event_bene_search ADD COLUMN aws_id VARCHAR(255);
ALTER TABLE event_error ADD COLUMN aws_id VARCHAR(255);
ALTER TABLE event_file ADD COLUMN aws_id VARCHAR(255);
ALTER TABLE event_job_status_change ADD COLUMN aws_id VARCHAR(255);

--rollback  ALTER TABLE event_api_request DROP COLUMN aws_id;
--rollback  ALTER TABLE event_api_response DROP COLUMN aws_id;
--rollback  ALTER TABLE event_bene_reload DROP COLUMN aws_id;
--rollback  ALTER TABLE event_bene_search DROP COLUMN aws_id;
--rollback  ALTER TABLE event_error ADD DROP aws_id;
--rollback  ALTER TABLE event_file ADD DROP aws_id;
--rollback  ALTER TABLE event_job_status_change DROP COLUMN aws_id;

--  -------------------------------------------------------------------------------------------------------------------



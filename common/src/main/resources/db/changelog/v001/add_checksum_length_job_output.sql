--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:add_checksum_length_job_output failOnError:true
ALTER TABLE job_output ADD COLUMN checksum VARCHAR(64) NOT NULL;
ALTER TABLE job_output ADD COLUMN file_length INTEGER NOT NULL;

--rollback  ALTER TABLE job_output DROP COLUMN IF EXISTS checksum; ALTER TABLE job_output DROP COLUMN IF EXISTS file_length;

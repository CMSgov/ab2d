--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:add_downloaded_to_job_output failOnError:true

ALTER TABLE job_output ADD COLUMN downloaded boolean NOT NULL DEFAULT false;

ALTER TABLE job_output ADD CONSTRAINT "uc_job_id_file_path" UNIQUE (job_id, file_path);
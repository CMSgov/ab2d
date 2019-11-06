--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:AB2D-356-ChangeColumnConstraintNames failOnError:true dbms:postgresql
ALTER TABLE job DROP CONSTRAINT uc_job_job_id;
ALTER TABLE job RENAME job_id TO job_uuid;
ALTER TABLE job ADD CONSTRAINT "uc_job_job_uuid" UNIQUE (job_uuid);
ALTER TABLE contract RENAME contract_id TO contract_number;
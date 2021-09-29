--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_started_by_column_to_job failOnError:true

ALTER TABLE job ADD COLUMN started_by VARCHAR(32) NOT NULL DEFAULT 'PDP';

--rollback  ALTER TABLE job DROP COLUMN manual
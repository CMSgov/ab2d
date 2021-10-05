--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:support_default_since failOnError:true

ALTER TABLE job ADD since_source VARCHAR(256);

UPDATE job SET since_source = 'USER' where since IS NOT NULL;
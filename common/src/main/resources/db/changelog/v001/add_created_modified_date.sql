--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset enolan:add_created_modified_date failOnError:true
ALTER TABLE beneficiary
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;


-- noinspection SqlWithoutWhere
UPDATE beneficiary SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE role
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;


-- noinspection SqlWithoutWhere
UPDATE role SET created='2000-01-01', modified='2000-01-01';


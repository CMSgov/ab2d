--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

ALTER TABLE role
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;


-- noinspection SqlWithoutWhere
UPDATE role SET created='2000-01-01', modified='2000-01-01';


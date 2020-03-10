--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:ab2d-1144-add-since-val failOnError:true
--  -------------------------------------------------------------------------------------------------------------------

alter table job add column since TIMESTAMP WITH TIME ZONE;

--rollback ALTER TABLE job DROP COLUMN IF EXISTS since;
--  -------------------------------------------------------------------------------------------------------------------


--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:add_attestor_role failOnError:true
INSERT INTO role (id, name) VALUES ((select nextval('hibernate_sequence')), 'ATTESTOR');
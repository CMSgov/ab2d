--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-2921 failOnError:true context:test

INSERT INTO role (id, name) VALUES ((select nextval('hibernate_sequence')), 'SPONSOR');
INSERT INTO role (id, name) VALUES ((select nextval('hibernate_sequence')), 'ADMIN');

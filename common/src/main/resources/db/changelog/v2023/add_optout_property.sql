--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'OptOutOn', 'false');


--rollback DELETE FROM properties WHERE key = 'OptOutOn';
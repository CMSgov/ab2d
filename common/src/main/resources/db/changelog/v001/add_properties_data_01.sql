--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lhanekam:add_properties_data_01 failOnError:true

INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'worker.engaged', 'engaged');


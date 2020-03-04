--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:add_contract2benecache_toggle_in_property_table failOnError:true
INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'ContractToBeneCachingOn', 'false');


--rollback DELETE FROM properties WHERE key = 'ContractToBeneCachingOn';


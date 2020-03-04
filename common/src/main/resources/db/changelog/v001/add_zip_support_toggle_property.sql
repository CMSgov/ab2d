--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:ab2d_1110_add_zip_support_toggle_in_property_table failOnError:true
INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'ZipSupportOn', 'false');

--rollback DELETE FROM properties WHERE key = 'ZipSupportOn';


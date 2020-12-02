--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_property_coverage_search failOnError:true

INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'coverage.update.engaged', 'idle');
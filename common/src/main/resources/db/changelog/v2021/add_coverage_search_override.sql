--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_coverage_search_override failOnError:true

INSERT INTO properties(id, key, value, created, modified)
VALUES (nextval('hibernate_sequence'), 'coverage.update.override', 'false', current_timestamp, current_timestamp);
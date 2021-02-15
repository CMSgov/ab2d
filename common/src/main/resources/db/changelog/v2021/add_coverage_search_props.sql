--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_coverage_search_props failOnError:true

INSERT INTO properties(id, key, value, created, modified)
VALUES (nextval('hibernate_sequence'), 'coverage.update.months.past', '1', current_timestamp, current_timestamp),
       (nextval('hibernate_sequence'), 'coverage.update.stale.days', '7', current_timestamp, current_timestamp),
       (nextval('hibernate_sequence'), 'coverage.update.stuck.hours', '24', current_timestamp, current_timestamp)
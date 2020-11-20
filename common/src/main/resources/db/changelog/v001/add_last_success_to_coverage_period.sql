--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_last_success_to_coverage_period failOnError:true

ALTER TABLE bene_coverage_period ADD COLUMN last_successful_job TIMESTAMP WITH TIME ZONE DEFAULT NULL;
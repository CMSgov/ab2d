--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_patient_eob_count failOnError:true

ALTER TABLE event_bene_search ADD COLUMN benes_with_eobs INT DEFAULT 0;
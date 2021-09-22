--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:rework_bene_search_event failOnError:true


ALTER TABLE event_bene_search RENAME num_in_contract TO benes_expected;
ALTER TABLE event_bene_search RENAME num_searched TO benes_searched;
ALTER TABLE event_bene_search RENAME num_errors TO benes_errored;

ALTER TABLE event_bene_search ADD COLUMN benes_queued INTEGER DEFAULT 0;
ALTER TABLE event_bene_search ADD COLUMN eobs_fetched INTEGER DEFAULT 0;
ALTER TABLE event_bene_search ADD COLUMN eobs_written INTEGER DEFAULT 0;
ALTER TABLE event_bene_search ADD COLUMN eob_files INTEGER DEFAULT 0;
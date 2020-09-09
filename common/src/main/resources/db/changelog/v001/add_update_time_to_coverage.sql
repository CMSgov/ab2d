--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_update_time_to_coverage failOnError:true

-- Every coverage job is uniquely identified by a contract_id, month, and year
CREATE TABLE bene_coverage_search
(
    id SERIAL NOT NULL,
    contract_id BIGINT NOT NULL,
    month INTEGER NOT NULL,
    year INTEGER NOT NULL,
    status VARCHAR(255)
);

ALTER TABLE bene_coverage_search ADD CONSTRAINT "pk_bene_coverage_search" PRIMARY KEY (id);
ALTER TABLE bene_coverage_search ADD CONSTRAINT "fk_bene_coverage_search_to_contract" FOREIGN KEY (contract_id) REFERENCES contract(id);
ALTER TABLE bene_coverage_search ADD CONSTRAINT "unique_bene_coverage_search_to_contract" UNIQUE (contract_id, month, year);

-- Drop the coverage table by first dropping all constraints and then the table
-- The table should be empty so there is no loss of data

DROP INDEX "ix_coverage_contract_id_month";
ALTER TABLE coverage DROP CONSTRAINT "pk_coverage";
ALTER TABLE coverage DROP CONSTRAINT "fk_coverage_to_beneficiary";
ALTER TABLE coverage DROP CONSTRAINT "fk_coverage_to_contract";
ALTER TABLE coverage DROP CONSTRAINT "uc_coverage_contract_id_beneficiary_id_month";

DROP TABLE coverage;

-- Beneficiary is only used in coverage so table is no longer necessary

DROP TABLE beneficiary;

-- Create table tracking coverage events for history

CREATE TABLE event_bene_coverage_search_status_change
(
    id SERIAL,
    bene_coverage_search_id INTEGER NOT NULL,
    time_of_event TIMESTAMP WITH TIME ZONE,
    old_status VARCHAR(255),
    new_status VARCHAR(255) NOT NULL,
    description TEXT
);

ALTER TABLE event_bene_coverage_search_status_change ADD CONSTRAINT "pk_event_bene_coverage_search" PRIMARY KEY (id);
ALTER TABLE event_bene_coverage_search_status_change ADD CONSTRAINT "fk_bene_coverage_search_update_to_bene_coverage_search" FOREIGN KEY (bene_coverage_search_id) REFERENCES bene_coverage_search(id);
CREATE INDEX "ix_bene_coverage_search_status_job_id" ON event_bene_coverage_search_status_change(bene_coverage_search_id);

-- Instead of writing out year, month, and contract id use a foreign key to bene_coverage_search

CREATE TABLE coverage
(
    id BIGSERIAL NOT NULL,
    bene_coverage_search_id INTEGER NOT NULL,
    bene_coverage_search_event_id INTEGER NOT NULL,
    beneficiary_id VARCHAR(255) NOT NULL
);

ALTER TABLE coverage ADD CONSTRAINT "pk_coverage" PRIMARY KEY (id);
ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_bene_coverage_search" FOREIGN KEY (bene_coverage_search_id) REFERENCES bene_coverage_search(id);
ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_bene_coverage_search_event" FOREIGN KEY (bene_coverage_search_event_id) REFERENCES event_bene_coverage_search_status_change(id);
CREATE INDEX "ix_coverage_search_and_event_id" ON coverage(bene_coverage_search_id, bene_coverage_search_event_id);
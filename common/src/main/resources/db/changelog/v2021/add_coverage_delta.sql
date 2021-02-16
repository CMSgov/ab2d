--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset enolan:add_coverage_delta_table failOnError:true
CREATE TABLE coverage_delta (
    id BIGSERIAL NOT NULL,
    bene_coverage_period_id INTEGER NOT NULL,
    beneficiary_id VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL, -- ADDED/REMOVED
    created timestamp
);

ALTER TABLE coverage_delta ADD CONSTRAINT "pk_coverage_delta" PRIMARY KEY (id);
ALTER TABLE coverage_delta ADD CONSTRAINT "fk_coverage_delta_bene_coverage_period"
    FOREIGN KEY (bene_coverage_period_id) REFERENCES bene_coverage_period(id);

--rollback  DROP TABLE coverage_delta;
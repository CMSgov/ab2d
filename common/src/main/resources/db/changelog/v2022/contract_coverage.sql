--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset enolan:contract_bus_key failOnError:true

-- Create and populate the contract business key
ALTER TABLE bene_coverage_period ADD COLUMN contract_number VARCHAR(255);
UPDATE bene_coverage_period
SET contract_number = c.contract_number
FROM contract c
WHERE contract_id = c.id;
-- There are two bad data cases in the bene_coverage_period table in production.  Put something there.
UPDATE bene_coverage_period
SET contract_number = 'UNKNOWN'
WHERE contract_id is null;
-- Enforce that a saved bene_coverage_period has a contract number
ALTER TABLE bene_coverage_period ALTER "contract_number" SET NOT NULL;

-- Foreign key is no longer needed.  The business key will be used going forward
ALTER TABLE bene_coverage_period DROP CONSTRAINT fk_bene_coverage_period_to_contract;
ALTER TABLE bene_coverage_period DROP COLUMN contract_id;


--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset enolan:contract_bus_key failOnError:true

-- Create and populate the contract business key
ALTER TABLE job ADD COLUMN contract_number VARCHAR(255);
UPDATE job
SET contract_number = c.contract_number
FROM contract c
WHERE contract_id is not null and contract_id = c.id;
-- There are two bad data cases in the job table in production.  Put something there.
UPDATE job
SET contract_number = 'UNKNOWN'
WHERE contract_id is null;
-- Enforce that a saved job has a contract number
ALTER TABLE job ALTER "contract_number" SET NOT NULL;

-- Foreign key is no longer needed.  The business key will be used going forward
ALTER TABLE job DROP CONSTRAINT fk_job_to_contract;
ALTER TABLE job DROP COLUMN contract_id;


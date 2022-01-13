--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset enolan:contract_bus_key failOnError:true

ALTER TABLE job ADD COLUMN contract_number VARCHAR(255);
UPDATE job
SET contract_number = c.contract_number
FROM contract c
WHERE contract_id is not null and contract_id = c.id;
-- There are two bad data cases in the job table in production.  Put something there.
UPDATE job
SET contract_number = 'UNKNOWN'
WHERE contract_id is null;
ALTER TABLE job ALTER "contract_number" SET NOT NULL;


ALTER TABLE job DROP CONSTRAINT fk_job_contract;
ALTER TABLE job DROP COLUMN contract_id;


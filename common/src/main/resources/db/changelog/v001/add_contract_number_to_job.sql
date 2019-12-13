ALTER TABLE job ADD COLUMN contract_id BIGINT DEFAULT NULL;
ALTER TABLE job ADD CONSTRAINT "fk_job_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);
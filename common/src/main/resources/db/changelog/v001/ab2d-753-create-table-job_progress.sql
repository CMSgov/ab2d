--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset spathiyil:AB2D-753-CreateTable-job_progress failOnError:true
CREATE TABLE job_progress
(
    id                  BIGINT              NOT NULL,
    job_id              BIGINT              NOT NULL,
    contract_id         BIGINT              NOT NULL,
    slice_number        INTEGER             NOT NULL,
    record_count        INTEGER             NOT NULL,
    progress            INTEGER
);

ALTER TABLE job_progress ADD CONSTRAINT "pk_job_progress" PRIMARY KEY (id);
ALTER TABLE job_progress ADD CONSTRAINT "uc_job_progress_job_contract_slice" UNIQUE (job_id, contract_id, slice_number);
ALTER TABLE job_progress ADD CONSTRAINT "fk_job_progress_to_job" FOREIGN KEY (job_id) REFERENCES job (id);
ALTER TABLE job_progress ADD CONSTRAINT "fk_job_progress_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);


--rollback DROP TABLE job_progress;
--  -------------------------------------------------------------------------------------------------------------------


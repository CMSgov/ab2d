--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset spathiyil:ab2d-1069-drop-coverage-table failOnError:true
DROP TABLE coverage;

--rollback CREATE TABLE coverage
--rollback (
--rollback     beneficiary_id      BIGINT              NOT NULL,
--rollback     contract_id         BIGINT              NOT NULL
--rollback );
--rollback ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_beneficiary" FOREIGN KEY (beneficiary_id) REFERENCES beneficiary (id);
--rollback ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);
--rollback ALTER TABLE coverage ADD CONSTRAINT "uc_coverage_beneficiary_id_contract_id" UNIQUE (beneficiary_id, contract_id);

--  -------------------------------------------------------------------------------------------------------------------

--changeset spathiyil:ab2d-1069-recreate-coverage-table-with-id-and-month failOnError:true
CREATE TABLE coverage
(
    id                  BIGINT              NOT NULL,
    contract_id         BIGINT              NOT NULL,
    beneficiary_id      BIGINT              NOT NULL,
    month               INTEGER             NOT NULL
);

ALTER TABLE coverage ADD CONSTRAINT "pk_coverage" PRIMARY KEY (id);
ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_beneficiary" FOREIGN KEY (beneficiary_id) REFERENCES beneficiary (id);
ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);
ALTER TABLE coverage ADD CONSTRAINT "uc_coverage_contract_id_beneficiary_id_month" UNIQUE (contract_id, beneficiary_id, month);

CREATE INDEX "ix_coverage_contract_id_month" ON coverage (contract_id, month);

--rollback  DROP TABLE coverage;
--  -------------------------------------------------------------------------------------------------------------------
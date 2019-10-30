--liquibase formatted sql

--  ---------------------------------------------------------------------------------------------------------------------
--changeset spathiyil:AB2D-291-CreateTable-beneficiary failOnError:true dbms:postgresql
--  ----------------------------------------------------------------------------------------------------------------------
CREATE TABLE beneficiary
(
    id              BIGSERIAL,
    patient_id      VARCHAR(255)        NOT NULL
);

ALTER TABLE beneficiary ADD CONSTRAINT "pk_beneficiary" PRIMARY KEY (id);
ALTER TABLE beneficiary ADD CONSTRAINT "uc_beneficiary_patient_id" UNIQUE (patient_id);

--rollback DROP TABLE beneficiary;

--  ----------------------------------------------------------------------------------------------------------------------
--changeset spathiyil:AB2D-291-CreateTable-contract failOnError:true dbms:postgresql
--  ----------------------------------------------------------------------------------------------------------------------
CREATE TABLE contract
(
    id              BIGSERIAL,
    contract_id     VARCHAR(255)        NOT NULL
);

ALTER TABLE contract ADD CONSTRAINT "pk_contract" PRIMARY KEY (id);
ALTER TABLE contract ADD CONSTRAINT "uc_contract_contract_id" UNIQUE (contract_id);

--rollback DROP TABLE contract;

--  ----------------------------------------------------------------------------------------------------------------------
--changeset spathiyil:AB2D-291-CreateTable-coverage failOnError:true dbms:postgresql
--  ----------------------------------------------------------------------------------------------------------------------
CREATE TABLE coverage
(
    beneficiary_id  BIGINT       NOT NULL,
    contract_id  	BIGINT       NOT NULL
);

ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_beneficiary" FOREIGN KEY (beneficiary_id) REFERENCES beneficiary (id);
ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);
ALTER TABLE coverage ADD CONSTRAINT "uc_coverage_beneficiary_id_contract_id" UNIQUE (beneficiary_id, contract_id);

--rollback  DROP TABLE coverage;

--  ----------------------------------------------------------------------------------------------------------------------
--changeset spathiyil:AB2D-291-CreateTable-sponsor failOnError:true dbms:postgresql
--  ----------------------------------------------------------------------------------------------------------------------
CREATE TABLE sponsor
(
    id              BIGSERIAL,
    hpms_id  	    INTEGER        	NOT NULL,
	legal_name	    VARCHAR(255)	NOT NULL,
	org_name	    VARCHAR(255)	NOT NULL,
	parent_id	    BIGINT
);

ALTER TABLE sponsor ADD CONSTRAINT "pk_sponsor" PRIMARY KEY (id);
ALTER TABLE sponsor ADD CONSTRAINT "uc_sponsor_hpms_id" UNIQUE (hpms_id);

-- --- NEED TO ADD A SELF-REFERENTIAL FOREIGN_KEY FOR PARENT_ID HERE

--rollback DROP TABLE sponsor;

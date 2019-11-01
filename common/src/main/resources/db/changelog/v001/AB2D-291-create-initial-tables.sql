--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateSequence-hibernate_sequence failOnError:true dbms:postgresql
CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;


--changeset spathiyil:AB2D-291-CreateTable-beneficiary failOnError:true dbms:postgresql
CREATE TABLE beneficiary
(
    id                  BIGINT              NOT NULL,
    patient_id          VARCHAR(255)        NOT NULL
);

ALTER TABLE beneficiary ADD CONSTRAINT "pk_beneficiary" PRIMARY KEY (id);
ALTER TABLE beneficiary ADD CONSTRAINT "uc_beneficiary_patient_id" UNIQUE (patient_id);

--rollback DROP TABLE beneficiary;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-contract failOnError:true dbms:postgresql
CREATE TABLE contract
(
    id                  BIGINT              NOT NULL,
    contract_id         VARCHAR(255)        NOT NULL
);

ALTER TABLE contract ADD CONSTRAINT "pk_contract" PRIMARY KEY (id);
ALTER TABLE contract ADD CONSTRAINT "uc_contract_contract_id" UNIQUE (contract_id);

--rollback DROP TABLE contract;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-coverage failOnError:true dbms:postgresql
CREATE TABLE coverage
(
    beneficiary_id      BIGINT              NOT NULL,
    contract_id         BIGINT              NOT NULL
);

ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_beneficiary" FOREIGN KEY (beneficiary_id) REFERENCES beneficiary (id);
ALTER TABLE coverage ADD CONSTRAINT "fk_coverage_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);
ALTER TABLE coverage ADD CONSTRAINT "uc_coverage_beneficiary_id_contract_id" UNIQUE (beneficiary_id, contract_id);

--rollback  DROP TABLE coverage;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-sponsor failOnError:true dbms:postgresql
CREATE TABLE sponsor
(
    id                  BIGINT              NOT NULL,
    hpms_id             INTEGER             NOT NULL,
    legal_name          VARCHAR(255)        NOT NULL,
    org_name            VARCHAR(255)        NOT NULL,
    parent_id           BIGINT
);

ALTER TABLE sponsor ADD CONSTRAINT "pk_sponsor" PRIMARY KEY (id);
ALTER TABLE sponsor ADD CONSTRAINT "uc_sponsor_hpms_id" UNIQUE (hpms_id);
ALTER TABLE sponsor ADD CONSTRAINT "fk_sponsor_to_sponsor_parent" FOREIGN KEY (parent_id) REFERENCES sponsor (id);

--rollback DROP TABLE sponsor;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-attestation failOnError:true dbms:postgresql
CREATE TABLE attestation
(
    id                  BIGINT              NOT NULL,
    sponsor_id          BIGINT              NOT NULL,
    contract_id         BIGINT              NOT NULL,
    attested_on         TIMESTAMPTZ         NOT NULL
);

ALTER TABLE attestation ADD CONSTRAINT "pk_attestation" PRIMARY KEY (id);
ALTER TABLE attestation ADD CONSTRAINT "fk_attestation_to_sponsor"  FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);
ALTER TABLE attestation ADD CONSTRAINT "fk_attestation_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);

--rollback DROP TABLE attestation;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-user_account failOnError:true dbms:postgresql
CREATE TABLE user_account
(
    id                  BIGINT              NOT NULL,
    user_name           VARCHAR(64)         NOT NULL,
    first_name          VARCHAR(64)         NOT NULL,
    last_name           VARCHAR(64)         NOT NULL,
    email               VARCHAR(255)        NOT NULL,
    sponsor_id          BIGINT              NOT NULL,
    enabled             BOOLEAN             NOT NULL
);

ALTER TABLE user_account ADD CONSTRAINT "pk_user_account" PRIMARY KEY (id);
ALTER TABLE user_account ADD CONSTRAINT "uc_user_account_user_name" UNIQUE (user_name);
ALTER TABLE user_account ADD CONSTRAINT "uc_user_account_email" UNIQUE (email);
ALTER TABLE user_account ADD CONSTRAINT "fk_user_account_to_sponsor"  FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);


--rollback DROP TABLE user_account;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-role failOnError:true dbms:postgresql
CREATE TABLE role
(
    id                  BIGINT              NOT NULL,
    name                VARCHAR(64)         NOT NULL
);

ALTER TABLE role ADD CONSTRAINT "pk_role" PRIMARY KEY (id);
ALTER TABLE role ADD CONSTRAINT "uc_role_name" UNIQUE (name);


--rollback DROP TABLE role;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-user_role failOnError:true dbms:postgresql
CREATE TABLE user_role
(
    user_account_id     BIGINT              NOT NULL,
    role_id             BIGINT              NOT NULL
);

ALTER TABLE user_role ADD CONSTRAINT "fk_user_role_to_user_account" FOREIGN KEY (user_account_id) REFERENCES user_account (id);
ALTER TABLE user_role ADD CONSTRAINT "fk_user_role_to_role" FOREIGN KEY (role_id) REFERENCES role (id);
ALTER TABLE user_role ADD CONSTRAINT "uc_user_role_user_account_id_role_id" UNIQUE (user_account_id, role_id);


--rollback DROP TABLE user_role;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-job_status failOnError:true dbms:postgresql
CREATE TABLE job_status
(
    id                  BIGINT              NOT NULL,
    name                VARCHAR(64)         NOT NULL
);

ALTER TABLE job_status ADD CONSTRAINT "pk_job_status" PRIMARY KEY (id);
ALTER TABLE job_status ADD CONSTRAINT "uc_job_status_name" UNIQUE (name);


--rollback DROP TABLE job_status;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-job failOnError:true dbms:postgresql
CREATE TABLE job
(
    id                  BIGINT              NOT NULL,
    job_id              VARCHAR(255)        NOT NULL,
    user_account_id     BIGINT              NOT NULL,
    created_at          TIMESTAMPTZ         NOT NULL,
    expires_at          TIMESTAMPTZ         NOT NULL,
    resource_types      VARCHAR(255)        NOT NULL,
--  job_status_id       BIGINT              NOT NULL,
    status              INT                 NOT NULL,
    status_message      TEXT,
    request_url         TEXT,
    progress            INT,
    last_poll_time      TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ
);

ALTER TABLE job ADD CONSTRAINT "pk_job" PRIMARY KEY (id);
ALTER TABLE job ADD CONSTRAINT "uc_job_job_id" UNIQUE (job_id);

ALTER TABLE job ADD CONSTRAINT "fk_job_to_user_account" FOREIGN KEY (user_account_id) REFERENCES user_account (id);
--ALTER TABLE job ADD CONSTRAINT "fk_job_to_job_status" FOREIGN KEY (job_status_id) REFERENCES job_status (id);

--rollback DROP TABLE job;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-job_output failOnError:true dbms:postgresql
CREATE TABLE job_output
(
    id                  BIGINT              NOT NULL,
    job_id              BIGINT              NOT NULL,
    file_path           TEXT                NOT NULL,
    fhir_resource_type  VARCHAR(255)        NOT NULL,
    error               BOOLEAN             NOT NULL
);

ALTER TABLE job_output ADD CONSTRAINT "pk_job_output" PRIMARY KEY (id);
ALTER TABLE job_output ADD CONSTRAINT "fk_job_output_to_job" FOREIGN KEY (job_id) REFERENCES job (id);

--rollback DROP TABLE job_output


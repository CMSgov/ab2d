--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateSequence-hibernate_sequence failOnError:true 
CREATE SEQUENCE hibernate_sequence START WITH 1 INCREMENT BY 1;


--changeset spathiyil:AB2D-291-CreateTable-beneficiary failOnError:true 
CREATE TABLE beneficiary
(
    id                  BIGINT              NOT NULL,
    patient_id          VARCHAR(255)        NOT NULL
);

ALTER TABLE beneficiary ADD CONSTRAINT "pk_beneficiary" PRIMARY KEY (id);
ALTER TABLE beneficiary ADD CONSTRAINT "uc_beneficiary_patient_id" UNIQUE (patient_id);

--rollback DROP TABLE beneficiary;
--  -------------------------------------------------------------------------------------------------------------------

--changeset spathiyil:AB2D-291-CreateTable-sponsor failOnError:true 
CREATE TABLE sponsor
(
    id                  BIGINT              NOT NULL,
    hpms_id             INTEGER             NOT NULL,
    org_name            VARCHAR(255)        NOT NULL,
    legal_name          VARCHAR(255),
    parent_id           BIGINT
);

ALTER TABLE sponsor ADD CONSTRAINT "pk_sponsor" PRIMARY KEY (id);
ALTER TABLE sponsor ADD CONSTRAINT "fk_sponsor_to_sponsor_parent" FOREIGN KEY (parent_id) REFERENCES sponsor (id);

--rollback DROP TABLE sponsor;
--  -------------------------------------------------------------------------------------------------------------------



--changeset spathiyil:AB2D-291-CreateTable-contract failOnError:true 
CREATE TABLE contract
(
    id                  BIGINT              NOT NULL,
    contract_number     VARCHAR(255)        NOT NULL,
    contract_name       VARCHAR(255)        NOT NULL,
    sponsor_id          BIGINT NOT NULL,
    attested_on         TIMESTAMP WITH TIME ZONE
);

ALTER TABLE contract ADD CONSTRAINT "pk_contract" PRIMARY KEY (id);
ALTER TABLE contract ADD CONSTRAINT "uc_contract_contract_number" UNIQUE (contract_number);
ALTER TABLE contract ADD CONSTRAINT "fk_contract_to_sponsor" FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);

--rollback DROP TABLE contract;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-coverage failOnError:true 
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


--changeset spathiyil:AB2D-291-CreateTable-user_account failOnError:true 
CREATE TABLE user_account
(
    id                  BIGINT              NOT NULL,
    username           VARCHAR(64)         NOT NULL,
    first_name          VARCHAR(64),
    last_name           VARCHAR(64),
    email               VARCHAR(255),
    sponsor_id          BIGINT              NOT NULL,
    enabled             BOOLEAN             NOT NULL
);

ALTER TABLE user_account ADD CONSTRAINT "pk_user_account" PRIMARY KEY (id);
ALTER TABLE user_account ADD CONSTRAINT "uc_user_account_username" UNIQUE (username);
ALTER TABLE user_account ADD CONSTRAINT "uc_user_account_email" UNIQUE (email);
ALTER TABLE user_account ADD CONSTRAINT "fk_user_account_to_sponsor"  FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);


--rollback DROP TABLE user_account;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-role failOnError:true 
CREATE TABLE role
(
    id                  BIGINT              NOT NULL,
    name                VARCHAR(64)         NOT NULL
);

ALTER TABLE role ADD CONSTRAINT "pk_role" PRIMARY KEY (id);
ALTER TABLE role ADD CONSTRAINT "uc_role_name" UNIQUE (name);


--rollback DROP TABLE role;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-user_role failOnError:true 
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


--changeset spathiyil:AB2D-291-CreateTable-job failOnError:true 
CREATE TABLE job
(
    id                  BIGINT              NOT NULL,
    job_uuid            VARCHAR(255)        NOT NULL,
    user_account_id     BIGINT              NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE         NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE,
    resource_types      VARCHAR(255),
    status              VARCHAR(32)         NOT NULL,
    status_message      TEXT,
    request_url         TEXT,
    progress            INT,
    last_poll_time      TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE
);

ALTER TABLE job ADD CONSTRAINT "pk_job" PRIMARY KEY (id);
ALTER TABLE job ADD CONSTRAINT uc_job_job_uuid UNIQUE (job_uuid);

ALTER TABLE job ADD CONSTRAINT "fk_job_to_user_account" FOREIGN KEY (user_account_id) REFERENCES user_account (id);

--rollback DROP TABLE job;
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-291-CreateTable-job_output failOnError:true 
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

--rollback DROP TABLE job_output;

--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-514-CreateTable-consent failOnError:true
CREATE TABLE consent
(
    id                  BIGINT                      NOT NULL,
    hicn                VARCHAR(64)                 NOT NULL,
    effective_date      DATE                        NOT NULL,
    policy_code         VARCHAR(255)                NOT NULL,
    purpose_code        VARCHAR(255)                NOT NULL,
    lo_inc_code         VARCHAR(255)                NOT NULL,
    scope_code          VARCHAR(255)                NOT NULL
);

ALTER TABLE consent ADD CONSTRAINT "pk_consent" PRIMARY KEY (id);

CREATE INDEX "ix_consent_hicn" ON consent (hicn);

--rollback DROP TABLE consent;
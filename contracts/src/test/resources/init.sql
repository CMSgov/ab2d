CREATE SCHEMA contract;

CREATE TABLE contract.contract (
                                   id bigint NOT NULL,
                                   contract_number character varying(255) NOT NULL,
                                   contract_name character varying(255) NOT NULL,
                                   attested_on timestamp with time zone,
                                   created timestamp without time zone,
                                   modified timestamp without time zone,
                                   hpms_parent_org_id bigint,
                                   hpms_parent_org_name character varying(255),
                                   hpms_org_marketing_name character varying(255),
                                   update_mode character varying(32) DEFAULT 'AUTOMATIC'::character varying NOT NULL,
                                   contract_type character varying(255) DEFAULT 'NORMAL'::character varying NOT NULL
);

ALTER TABLE ONLY contract.contract
    ADD CONSTRAINT contract_contract_number_key UNIQUE (contract_number);

ALTER TABLE ONLY contract.contract
    ADD CONSTRAINT contract_pkey PRIMARY KEY (id);

CREATE SEQUENCE hibernate_sequence START 1;
CREATE SEQUENCE IF NOT EXISTS contract_seq START 1;
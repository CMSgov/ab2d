CREATE TABLE IF NOT EXISTS contract.contract
(
    id bigint NOT NULL,
    contract_number character varying(255) COLLATE pg_catalog."default" NOT NULL,
    contract_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    attested_on timestamp with time zone,
                              created timestamp without time zone,
                              modified timestamp without time zone,
                              hpms_parent_org_id bigint,
                              hpms_parent_org_name character varying(255) COLLATE pg_catalog."default",
    hpms_org_marketing_name character varying(255) COLLATE pg_catalog."default",
    update_mode character varying(32) COLLATE pg_catalog."default" NOT NULL DEFAULT 'AUTOMATIC'::character varying,
    contract_type character varying(255) COLLATE pg_catalog."default" NOT NULL DEFAULT 'NORMAL'::character varying,
    CONSTRAINT contract_pkey PRIMARY KEY (id),
    CONSTRAINT contract_contract_number_key UNIQUE (contract_number)
    )

    TABLESPACE pg_default;

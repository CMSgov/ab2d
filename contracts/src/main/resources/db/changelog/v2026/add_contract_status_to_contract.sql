ALTER TABLE contract.contract
    ADD COLUMN IF NOT EXISTS contract_status character varying(64) COLLATE pg_catalog."default";

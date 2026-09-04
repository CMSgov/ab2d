ALTER TABLE contract.contract
    ADD COLUMN IF NOT EXISTS attestation_status character varying(64) COLLATE pg_catalog."default";

UPDATE contract.contract
SET attestation_status = CASE WHEN attested_on IS NULL THEN 'WITHOUT_ATTESTATION' ELSE 'ATTESTED' END
WHERE attestation_status IS NULL;

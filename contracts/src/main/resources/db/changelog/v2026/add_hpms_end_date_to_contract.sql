ALTER TABLE contract.contract
    ADD COLUMN IF NOT EXISTS hpms_end_date timestamp with time zone;

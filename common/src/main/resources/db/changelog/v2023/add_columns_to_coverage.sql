--changeset 5567 sadibhatla
-- adding additional columns to coverage table to support opt-out logic
-- TODO : figure out the flat file structure and columns expected as part of the flat file that we need to ingest
-- TODO : update more columns to add if needed

ALTER TABLE coverage ADD COLUMN IF NOT EXISTS opt_out_flag BOOLEAN DEFAULT 'FALSE';
ALTER TABLE coverage ADD COLUMN IF NOT EXISTS effective_date TIMESTAMP;
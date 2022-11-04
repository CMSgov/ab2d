-- Create the new schema
CREATE SCHEMA IF NOT EXISTS contract;

-- Create temporary table and populate it with all the properties from ab2d
CREATE TEMPORARY TABLE contract_tmp (like public.contract);
INSERT INTO contract_tmp (SELECT * FROM public.contract);

-- Create new table if it doesn't already exist
CREATE TABLE IF NOT EXISTS contract.contract (like public.contract including all);

-- Insert any missing values into the properties table. If this is run a second time, it will not duplicate the values
INSERT INTO contract.contract (id, contract_number, contract_name, attested_on, created, modified, hpms_parent_org_id, hpms_parent_org_name, hpms_org_marketing_name, update_mode, contract_type)
SELECT t.id, t.contract_number, t.contract_name, t.attested_on, t.created, t.modified, t.hpms_parent_org_id, t.hpms_parent_org_name, t.hpms_org_marketing_name, t.update_mode, t.contract_type
FROM contract_tmp t
         LEFT JOIN contract.contract p ON p.id = t.id WHERE p.id is null;

-- Drop the temp table - clean up
DROP TABLE contract_tmp;

-- Let Quicksight query this table if it ever wants to
GRANT SELECT ON contract.contract TO ab2d_analyst;

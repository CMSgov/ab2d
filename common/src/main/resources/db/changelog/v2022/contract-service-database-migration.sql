-- Create the new schema
CREATE SCHEMA IF NOT EXISTS contract;

-- Create temporary table and populate it with all the properties from ab2d
CREATE TEMPORARY TABLE contract_tmp (like public.contract);
INSERT INTO contract_tmp (SELECT * FROM public.contract);

-- Create new table if it doesn't already exist
CREATE TABLE IF NOT EXISTS contract.contract (like public.contract including all);

-- Insert any missing values into the contract table. If this is run a second time, it will not duplicate the values
INSERT INTO contract.contract (id, contract_number, contract_name, attested_on, created, modified, hpms_parent_org_id, hpms_parent_org_name, hpms_org_marketing_name, update_mode, contract_type)

SELECT t.id, t.contract_number, t.contract_name, t.attested_on, t.created, t.modified, t.hpms_parent_org_id, t.hpms_parent_org_name, t.hpms_org_marketing_name, t.update_mode, t.contract_type
FROM contract_tmp t
         LEFT JOIN contract.contract p ON p.id = t.id WHERE p.id is null;

-- Drop the temp table - clean up
DROP TABLE contract_tmp;

-- Let Quicksight query this table if it ever wants to
GRANT SELECT ON contract.contract TO ab2d_analyst;

ALTER TABLE public.user_account DROP CONSTRAINT fk_user_to_contract;

-- View: public.contract_view
-- DROP VIEW public.contract_view;
CREATE OR REPLACE VIEW public.contract_view
 AS
SELECT con.contract_number,
       con.contract_name,
       con.attested_on,
       con.created,
       con.modified,
       con.hpms_parent_org_name,
       con.hpms_org_marketing_name,
       con.update_mode,
       con.contract_type,
       u.enabled
FROM contract.contract con
         LEFT JOIN user_account u ON con.id = u.contract_id;

DO
$$
BEGIN
    --- Needed for tests that dont have cmsadmin
    IF EXISTS ( SELECT FROM pg_roles WHERE  rolname = 'cmsadmin') THEN
        ALTER TABLE public.contract_view OWNER TO cmsadmin;
        GRANT ALL ON TABLE public.contract_view TO cmsadmin;
    END IF;
END
$$;

GRANT SELECT ON TABLE public.contract_view TO ab2d_analyst;




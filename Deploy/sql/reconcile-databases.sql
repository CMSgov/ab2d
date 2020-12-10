-- Delete all applicable records

TRUNCATE public.sponsor CASCADE;
TRUNCATE public.contract CASCADE;
TRUNCATE public.user_account CASCADE;
TRUNCATE public.role CASCADE;
TRUNCATE public.user_role CASCADE;

-- Insert sponsor records

INSERT INTO public.sponsor(
  id, hpms_id, org_name, legal_name, parent_id, created, modified
)
SELECT a.id, a.hpms_id, a.org_name, a.legal_name, a.parent_id, a.created, a.modified
FROM temporary.sponsor a;

-- Insert contract records

INSERT INTO public.contract(
  id, contract_number, contract_name, sponsor_id, attested_on, created, modified, hpms_parent_org_id, hpms_parent_org_name, hpms_org_marketing_name
)
SELECT a.id, a.contract_number, a.contract_name, a.sponsor_id, a.attested_on, a.created, a.modified, a.hpms_parent_org_id, a.hpms_parent_org_name, a.hpms_org_marketing_name
FROM temporary.contract a;

-- Insert user account records

INSERT INTO public.user_account(
  id, username, first_name, last_name, email, sponsor_id, enabled, max_parallel_jobs, created, modified
)
SELECT a.id, a.username, a.first_name, a.last_name, a.email, a.sponsor_id, a.enabled, a.max_parallel_jobs, a.created, a.modified
FROM temporary.user_account a;

-- Insert role records

INSERT INTO public.role(
  id, name, created, modified
)
SELECT a.id, a.name, a.created, a.modified
FROM temporary.role a;

-- Insert user role records

INSERT INTO public.user_role(
  user_account_id, role_id
)
SELECT a.user_account_id, a.role_id
FROM temporary.user_role a;

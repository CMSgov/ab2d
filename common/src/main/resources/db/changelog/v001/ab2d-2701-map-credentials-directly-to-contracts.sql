-- Set notifications to 'warning' or greater

SET client_min_messages TO WARNING;

-- Drop uc_user_account_email constraint on user_account table

ALTER TABLE user_account DROP CONSTRAINT IF EXISTS uc_user_account_email;

-- Add contract_id to user_account table

ALTER TABLE user_account ADD COLUMN IF NOT EXISTS contract_id bigint;

-- Update existing user_account records with their correct sponsor id

UPDATE public.user_account h
SET sponsor_id = a.id
FROM public.sponsor a
INNER JOIN public.contract d
ON a.id = d.sponsor_id
INNER JOIN public.user_account e
ON a.id = e.sponsor_id OR a.parent_id = e.sponsor_id
WHERE h.id = e.id;

-- Update existing user_account records with their correct contract_id

UPDATE public.user_account h
SET contract_id = d.id
FROM public.sponsor a
INNER JOIN public.contract d
ON a.id = d.sponsor_id
INNER JOIN public.user_account e
ON a.id = e.sponsor_id
WHERE h.id = e.id;

-- Add 'not null' constraint to contract_id column of the user_account table

ALTER TABLE user_account
ALTER COLUMN contract_id SET NOT NULL;

-- Add foreign key to the user_account table that directly references the contract associated with the user account mapping

ALTER TABLE user_account
ADD CONSTRAINT "fk_user_account_to_contract"
FOREIGN KEY (contract_id)
REFERENCES contract (id);

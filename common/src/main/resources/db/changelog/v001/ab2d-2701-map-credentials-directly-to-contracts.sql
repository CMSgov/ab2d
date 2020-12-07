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

-- Update existing user_account records with their correct contract id

UPDATE public.user_account h
SET contract_id = d.id
FROM public.sponsor a
INNER JOIN public.contract d
ON a.id = d.sponsor_id
INNER JOIN public.user_account e
ON a.id = e.sponsor_id
WHERE h.id = e.id;



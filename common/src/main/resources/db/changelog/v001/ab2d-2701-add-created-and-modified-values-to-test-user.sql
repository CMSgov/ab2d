-- Add created and modified values to test user

UPDATE public.user_account
SET
  created = CURRENT_TIMESTAMP(6)::TIMESTAMP WITHOUT TIME ZONE,
  modified = CURRENT_TIMESTAMP(6)::TIMESTAMP WITHOUT TIME ZONE
WHERE username = 'EileenCFrierson@example.com';

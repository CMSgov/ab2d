-- CREATE SEQUENCE IF NOT EXISTS public.user_account_seq START 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS public.user_account_seq START 1;

select setval('public.user_account_seq', (SELECT MAX(id) FROM public.user_account));
ALTER TABLE public.user_account ALTER COLUMN id SET DEFAULT nextval('public.user_account_seq');
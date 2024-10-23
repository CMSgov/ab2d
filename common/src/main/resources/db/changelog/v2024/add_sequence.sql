CREATE SEQUENCE IF NOT EXISTS public.user_account_seq START 1 INCREMENT BY 50;

select setval('public.user_account_seq', (SELECT MAX(id) FROM public.user_account));
ALTER TABLE public.user_account ALTER COLUMN id SET DEFAULT nextval('public.user_account_seq');

CREATE SEQUENCE IF NOT EXISTS public.job_seq START 1;
CREATE SEQUENCE IF NOT EXISTS public.job_output_seq START 1;

CREATE SEQUENCE IF NOT EXISTS public.bene_coverage_period_seq START 1 INCREMENT BY 50;

select setval('public.bene_coverage_period_seq', (SELECT MAX(id) FROM public.bene_coverage_period));
ALTER TABLE public.bene_coverage_period ALTER COLUMN id SET DEFAULT nextval('public.bene_coverage_period_seq');

CREATE SEQUENCE IF NOT EXISTS public.coverage_search_seq START 1;
CREATE SEQUENCE IF NOT EXISTS public.event_bene_coverage_search_status_change_seq START 1;

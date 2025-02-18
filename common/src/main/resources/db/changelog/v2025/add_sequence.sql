CREATE SEQUENCE IF NOT EXISTS public.user_account_seq START 1 INCREMENT BY 1;
SELECT setval('public.user_account_seq', COALESCE((SELECT MAX(id) FROM public.user_account), 1), true);
ALTER TABLE public.user_account ALTER COLUMN id SET DEFAULT nextval('public.user_account_seq');

CREATE SEQUENCE IF NOT EXISTS public.job_seq START 1 INCREMENT BY 1;
SELECT setval('public.job_seq', COALESCE((SELECT MAX(id) FROM public.job), 1), true);
ALTER TABLE public.job ALTER COLUMN id SET DEFAULT nextval('public.job_seq');

CREATE SEQUENCE IF NOT EXISTS public.job_output_seq START 1 INCREMENT BY 1;
SELECT setval('public.job_output_seq', COALESCE((SELECT MAX(id) FROM public.job_output), 1), true);
ALTER TABLE public.job_output ALTER COLUMN id SET DEFAULT nextval('public.job_output_seq');

CREATE SEQUENCE IF NOT EXISTS public.event_bene_coverage_search_status_change_seq START 1 INCREMENT BY 1;
SELECT setval('public.event_bene_coverage_search_status_change_seq', COALESCE((SELECT MAX(id) FROM public.event_bene_coverage_search_status_change), 1), true);
ALTER TABLE public.event_bene_coverage_search_status_change ALTER COLUMN id SET DEFAULT nextval('public.event_bene_coverage_search_status_change_seq');

CREATE SEQUENCE IF NOT EXISTS public.bene_coverage_period_seq START 1 INCREMENT BY 1;
SELECT setval('public.bene_coverage_period_seq', COALESCE((SELECT MAX(id) FROM public.bene_coverage_period), 1), true);
ALTER TABLE public.bene_coverage_period ALTER COLUMN id SET DEFAULT nextval('public.bene_coverage_period_seq');

CREATE SEQUENCE IF NOT EXISTS public.coverage_search_seq START 1 INCREMENT BY 1;
SELECT setval('coverage_search_seq', COALESCE((SELECT MAX(id) FROM public.coverage_search), 1), true);
ALTER TABLE public.coverage_search ALTER COLUMN id SET DEFAULT nextval('public.coverage_search_seq');

CREATE SEQUENCE IF NOT EXISTS public.role_seq START 1 INCREMENT BY 1;
SELECT setval('public.role_seq', COALESCE((SELECT MAX(id) FROM public.role), 1), true);
ALTER TABLE public.role ALTER COLUMN id SET DEFAULT nextval('public.role_seq');

ALTER SEQUENCE IF EXISTS contract_seq INCREMENT BY 50;
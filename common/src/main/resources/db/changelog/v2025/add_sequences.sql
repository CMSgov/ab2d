CREATE SEQUENCE IF NOT EXISTS public.user_account_seq START 1 INCREMENT BY 1;
SELECT setval('public.user_account_seq', COALESCE((SELECT MAX(id) FROM public.user_account), 1), true);

CREATE SEQUENCE IF NOT EXISTS public.job_seq START 1 INCREMENT BY 1;
SELECT setval('public.job_seq', COALESCE((SELECT MAX(id) FROM public.job), 1), true);

CREATE SEQUENCE IF NOT EXISTS public.job_output_seq START 1 INCREMENT BY 1;
SELECT setval('public.job_output_seq', COALESCE((SELECT MAX(id) FROM public.job_output), 1), true);

CREATE SEQUENCE IF NOT EXISTS public.event_bene_coverage_search_status_change_seq START 1 INCREMENT BY 1;
SELECT setval('public.event_bene_coverage_search_status_change_seq', COALESCE((SELECT MAX(id) FROM public.event_bene_coverage_search_status_change), 1), true);

CREATE SEQUENCE IF NOT EXISTS public.bene_coverage_period_seq START 1 INCREMENT BY 1;
SELECT setval('public.bene_coverage_period_seq', COALESCE((SELECT MAX(id) FROM public.bene_coverage_period), 1), true);

CREATE SEQUENCE IF NOT EXISTS public.coverage_search_seq START 1 INCREMENT BY 1;
SELECT setval('coverage_search_seq', COALESCE((SELECT MAX(id) FROM public.coverage_search), 1), true);

CREATE SEQUENCE IF NOT EXISTS public.role_seq START 1 INCREMENT BY 1;
SELECT setval('public.role_seq', COALESCE((SELECT MAX(id) FROM public.role), 1), true);

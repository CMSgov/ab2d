ALTER TABLE public.user_account ALTER COLUMN id SET DEFAULT nextval('public.user_account_seq');

ALTER TABLE public.job ALTER COLUMN id SET DEFAULT nextval('public.job_seq');

ALTER TABLE public.job_output ALTER COLUMN id SET DEFAULT nextval('public.job_output_seq');

ALTER TABLE public.event_bene_coverage_search_status_change ALTER COLUMN id SET DEFAULT nextval('public.event_bene_coverage_search_status_change_seq');

ALTER TABLE public.bene_coverage_period ALTER COLUMN id SET DEFAULT nextval('public.bene_coverage_period_seq');

ALTER TABLE public.coverage_search ALTER COLUMN id SET DEFAULT nextval('public.coverage_search_seq');

ALTER TABLE public.role ALTER COLUMN id SET DEFAULT nextval('public.role_seq');

-- CREATE SEQUENCE IF NOT EXISTS public.bene_coverage_period_seq START 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS public.bene_coverage_period_seq START 1;

select setval('public.bene_coverage_period_seq', (SELECT MAX(id) FROM public.bene_coverage_period));
ALTER TABLE public.bene_coverage_period ALTER COLUMN id SET DEFAULT nextval('public.bene_coverage_period_seq');
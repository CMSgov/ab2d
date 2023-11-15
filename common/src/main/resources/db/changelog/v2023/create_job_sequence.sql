CREATE SEQUENCE IF NOT EXISTS public.job_seq START 1 INCREMENT BY 50;

select setval('public.job_seq', (SELECT MAX(id) FROM public.job));
ALTER TABLE public.job ALTER COLUMN id SET DEFAULT nextval('public.job_seq');
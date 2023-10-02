CREATE SEQUENCE IF NOT EXISTS public.job_output_seq START 1;

select setval('public.job_output_seq', (SELECT MAX(id) FROM public.job_output));
ALTER TABLE public.job_output ALTER COLUMN id SET DEFAULT nextval('public.job_output_seq');
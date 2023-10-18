CREATE SEQUENCE IF NOT EXISTS public.coverage_search_seq START 1 INCREMENT BY 50;

select setval('public.coverage_search_seq', (SELECT MAX(id) FROM public.coverage_search));
ALTER TABLE public.coverage_search ALTER COLUMN id SET DEFAULT nextval('public.coverage_search_seq');
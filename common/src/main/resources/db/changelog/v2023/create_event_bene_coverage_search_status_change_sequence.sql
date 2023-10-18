CREATE SEQUENCE IF NOT EXISTS public.event_bene_coverage_search_status_change_seq START 1 INCREMENT BY 50;

select setval('public.event_bene_coverage_search_status_change_seq', (SELECT MAX(id) FROM public.event_bene_coverage_search_status_change));
ALTER TABLE public.event_bene_coverage_search_status_change ALTER COLUMN id SET DEFAULT nextval('public.event_bene_coverage_search_status_change_seq');
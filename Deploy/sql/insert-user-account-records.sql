INSERT INTO public.user_account(
  id, username, first_name, last_name, email, sponsor_id, enabled, max_parallel_jobs, created, modified)
  VALUES (nextval('hibernate_sequence'), 'client id', 'Fred', 'Smith', 'fred.smith@nowhere.com', 3065, true, 1, CURRENT_TIMESTAMP(6)::TIMESTAMP WITHOUT TIME ZONE, CURRENT_TIMESTAMP(6)::TIMESTAMP WITHOUT TIME ZONE);

INSERT INTO public.user_account(
  id, username, first_name, last_name, email, sponsor_id, enabled, max_parallel_jobs, created, modified)
  VALUES (nextval('hibernate_sequence'), 'bjones', 'Barney', 'Jones', 'barney.jones@nowhere.com', 3065, false, 1, CURRENT_TIMESTAMP(6)::TIMESTAMP WITHOUT TIME ZONE, CURRENT_TIMESTAMP(6)::TIMESTAMP WITHOUT TIME ZONE);

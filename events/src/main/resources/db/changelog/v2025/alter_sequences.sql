ALTER TABLE event.event_api_request ALTER COLUMN id SET DEFAULT nextval('event.event_api_request_id_seq');

ALTER TABLE event.event_api_response ALTER COLUMN id SET DEFAULT nextval('event.event_api_response_id_seq');

ALTER TABLE event.event_bene_reload ALTER COLUMN id SET DEFAULT nextval('event.event_bene_reload_id_seq');

ALTER TABLE event.event_bene_search ALTER COLUMN id SET DEFAULT nextval('event.event_bene_search_id_seq');

ALTER TABLE event.event_error ALTER COLUMN id SET DEFAULT nextval('event.event_error_id_seq');

ALTER TABLE event.event_file ALTER COLUMN id SET DEFAULT nextval('event.event_file_id_seq');

ALTER TABLE event.event_job_status_change ALTER COLUMN id SET DEFAULT nextval('event.event_job_status_change_id_seq');
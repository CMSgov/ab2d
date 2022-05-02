CREATE SCHEMA IF NOT EXISTS event;

CREATE TABLE IF NOT EXISTS event.event_api_request (like public.event_api_request including all);
INSERT INTO event.event_api_request (SELECT * FROM public.event_api_request);


CREATE TABLE IF NOT EXISTS event.event_api_response (like public.event_api_response including all);
INSERT INTO event.event_api_response (SELECT * FROM public.event_api_response);


CREATE TABLE IF NOT EXISTS event.event_bene_coverage_search_status_change (like public.event_bene_coverage_search_status_change including all);
INSERT INTO event.event_bene_coverage_search_status_change (SELECT * FROM public.event_bene_coverage_search_status_change);

ALTER TABLE public.coverage DROP CONSTRAINT fk_coverage_to_bene_coverage_search_event;

-- Index: ix_bene_coverage_search_status_job_id
-- DROP INDEX IF EXISTS ix_bene_coverage_search_status_job_id;
CREATE INDEX IF NOT EXISTS ix_bene_coverage_search_status_job_id
    ON event.event_bene_coverage_search_status_change USING btree
        (bene_coverage_period_id ASC NULLS LAST);



CREATE TABLE IF NOT EXISTS event.event_bene_reload (like public.event_bene_reload including all);
INSERT INTO event.event_bene_reload (SELECT * FROM public.event_bene_reload);


CREATE TABLE IF NOT EXISTS event.event_bene_search (like public.event_bene_search including all);
INSERT INTO event.event_bene_search (SELECT * FROM public.event_bene_search);

CREATE INDEX IF NOT EXISTS ix_bene_search_job_id
    ON event.event_bene_search USING btree
        (job_id ASC NULLS LAST);


CREATE TABLE IF NOT EXISTS event.event_error (like public.event_error including all);
INSERT INTO event.event_error (SELECT * FROM public.event_error);


CREATE TABLE IF NOT EXISTS event.event_file (like public.event_file including all);
INSERT INTO event.event_file (SELECT * FROM public.event_file);

CREATE INDEX IF NOT EXISTS ix_file_event_job_id
    ON event.event_file USING btree
        (job_id ASC NULLS LAST);




CREATE TABLE IF NOT EXISTS event.event_job_status_change (like public.event_job_status_change including all);
INSERT INTO event.event_job_status_change (SELECT * FROM public.event_job_status_change);

CREATE INDEX IF NOT EXISTS ix_job_status_job_id
    ON event.event_job_status_change USING btree
        (job_id ASC NULLS LAST);


GRANT SELECT ON event.event_api_response TO ab2d_analyst;
GRANT SELECT ON event.event_api_request TO ab2d_analyst;
GRANT SELECT ON event.event_bene_coverage_search_status_change TO ab2d_analyst;
GRANT SELECT ON event.event_bene_reload TO ab2d_analyst;
GRANT SELECT ON event.event_bene_search TO ab2d_analyst;
GRANT SELECT ON event.event_error TO ab2d_analyst;
GRANT SELECT ON event.event_file TO ab2d_analyst;
GRANT SELECT ON event.event_job_status_change TO ab2d_analyst;
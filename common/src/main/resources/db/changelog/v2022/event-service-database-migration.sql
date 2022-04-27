CREATE SCHEMA IF NOT EXISTS event;

CREATE TABLE IF NOT EXISTS event.event_api_request (like public.event_api_request including all);
INSERT INTO event.event_api_request (SELECT * FROM public.event_api_request);
-- GRANT SELECT ON TABLE event.event_api_request TO ab2d_analyst;


CREATE TABLE IF NOT EXISTS event.event_api_response (like public.event_api_response including all);
INSERT INTO event.event_api_response (SELECT * FROM public.event_api_response);

-- GRANT SELECT ON TABLE event.event_api_response TO ab2d_analyst;


CREATE TABLE IF NOT EXISTS event.event_bene_coverage_search_status_change (like public.event_bene_coverage_search_status_change including all);
INSERT INTO event.event_bene_coverage_search_status_change (SELECT * FROM public.event_bene_coverage_search_status_change);

-- GRANT SELECT ON TABLE event.event_bene_coverage_search_status_change TO ab2d_analyst;

-- Index: ix_bene_coverage_search_status_job_id
-- DROP INDEX IF EXISTS ix_bene_coverage_search_status_job_id;
CREATE INDEX IF NOT EXISTS ix_bene_coverage_search_status_job_id
    ON event.event_bene_coverage_search_status_change USING btree
        (bene_coverage_period_id ASC NULLS LAST);


CREATE TABLE IF NOT EXISTS event.event_bene_reload (like public.event_bene_reload including all);
INSERT INTO event.event_bene_reload (SELECT * FROM public.event_bene_reload);

-- GRANT SELECT ON TABLE event.event_bene_reload TO ab2d_analyst;


CREATE TABLE IF NOT EXISTS event.event_bene_search (like public.event_bene_search including all);
INSERT INTO event.event_bene_search (SELECT * FROM public.event_bene_search);
-- GRANT SELECT ON TABLE event.event_bene_search TO ab2d_analyst;

CREATE INDEX IF NOT EXISTS ix_bene_search_job_id
    ON event.event_bene_search USING btree
        (job_id ASC NULLS LAST);


CREATE TABLE IF NOT EXISTS event.event_error (like public.event_error including all);
INSERT INTO event.event_error (SELECT * FROM public.event_error);
-- GRANT SELECT ON TABLE event.event_error TO ab2d_analyst;


CREATE TABLE IF NOT EXISTS event.event_file (like public.event_file including all);
INSERT INTO event.event_file (SELECT * FROM public.event_file);
-- GRANT SELECT ON TABLE event.event_file TO ab2d_analyst;

CREATE INDEX IF NOT EXISTS ix_file_event_job_id
    ON event.event_file USING btree
        (job_id ASC NULLS LAST);


CREATE TABLE IF NOT EXISTS event.event_job_status_change (like public.event_job_status_change including all);
INSERT INTO event.event_job_status_change (SELECT * FROM public.event_job_status_change);
-- GRANT SELECT ON TABLE event.event_job_status_change TO ab2d_analyst;
-- GRANT ALL ON TABLE event.event_job_status_change TO cmsadmin;
-- Index: ix_job_status_job_id
-- DROP INDEX IF EXISTS ix_job_status_job_id;
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
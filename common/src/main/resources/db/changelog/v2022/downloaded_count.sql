--changeset sb-keane:convert downloaded to integer, add file.download.count.max property,

alter table job_output
alter column downloaded DROP DEFAULT;
alter table job_output
alter column downloaded type integer
using case when downloaded = true then 1 else 0 end;
alter table job_output
alter column downloaded SET DEFAULT 0;

INSERT INTO properties (id, key, value, created) VALUES((select nextval('hibernate_sequence')), 'file.download.count.max', '6', now());
INSERT INTO properties (id, key, value, created) VALUES((select nextval('hibernate_sequence')), 'file.download.max.interval.minutes', '30', now());

alter table job_output
add column last_download_at timestamp null;
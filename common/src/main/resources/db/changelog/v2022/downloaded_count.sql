--changeset sb-keane:convert downloaded to integer, add last_download_at,

alter table job_output
alter column downloaded DROP DEFAULT;
alter table job_output
alter column downloaded type integer
using case when downloaded = true then 1 else 0 end;
alter table job_output
alter column downloaded SET DEFAULT 0;

alter table job_output
add column last_download_at timestamp null;
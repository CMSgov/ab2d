--changeset enolan:contract_bus_key failOnError:true

DROP VIEW job_view;

-- Create and populate the contract business key
ALTER TABLE job ADD COLUMN organization VARCHAR(255) NOT NULL DEFAULT '';

UPDATE job
SET organization = ua.organization
FROM user_account ua
WHERE job.user_account_id = ua.id;

-- Foreign key is no longer needed.  The business key will be used going forward
ALTER TABLE job DROP CONSTRAINT fk_job_to_user_account;
ALTER TABLE job DROP COLUMN user_account_id;

-- recreate job_view
-- Job view gives high level information concerning a job including contract_number and organization
-- but excludes any information about Okta client creds or internal database ids.
CREATE VIEW job_view AS
SELECT j.id, j.job_uuid, j.created_at, j.completed_at, j.expires_at, j.resource_types, j.status, j.request_url,
       j.output_format, j.since, j.fhir_version, tz_weeks.year_week, tz_weeks.week_start, tz_weeks.week_end,
       j.organization, c.contract_number, c.contract_name, c.contract_type
FROM job j
         -- Create a series of weeks that when joined give a specific week that a job was run in
-- This supports reporting weekly statistics
         INNER JOIN (
    SELECT week_start, week_start + INTERVAL '7' DAY AS week_end, to_char(week_start, 'IYYY-IW') year_week
    FROM (SELECT week_start AT TIME ZONE 'America/New_York' AS week_start
          FROM generate_series('2020-02-03 0:00:00.00+00', current_timestamp AT TIME ZONE 'America/New_York', '1 week'::INTERVAL) AS week_start
         ) AS tz_weeks
) AS tz_weeks ON j.created_at >= tz_weeks.week_start AND j.created_at < tz_weeks.week_end
         LEFT JOIN contract c ON c.contract_number = j.contract_number
-- Only report jobs that were started by a PDP since those jobs are the only ones we care about for analytics
WHERE j.started_by = 'PDP' and j.contract_number != 'UNKNOWN';
alter table job add column until TIMESTAMP WITH TIME ZONE;

CREATE
OR REPLACE VIEW job_view AS
SELECT j.id,
       j.job_uuid,
       j.created_at,
       j.completed_at,
       j.expires_at,
       j.resource_types,
       j.status,
       j.request_url,
       j.output_format,
       j.since,
       j.until,
       j.fhir_version,
       tz_weeks.year_week,
       tz_weeks.week_start,
       tz_weeks.week_end,
       j.organization,
       c.contract_number,
       c.contract_name,
       c.contract_type
FROM job j
         -- Create a series of weeks that when joined give a specific week that a job was run in
-- This supports reporting weekly statistics
         INNER JOIN (SELECT week_start,
                            week_start + INTERVAL '7' DAY AS week_end,
                            to_char(week_start, 'IYYY-IW')   year_week
                     FROM (SELECT week_start AT TIME ZONE 'America/New_York' AS week_start
                           FROM generate_series('2020-02-03 0:00:00.00+00',
                                                current_timestamp AT TIME ZONE 'America/New_York',
                                                '1 week'::INTERVAL) AS week_start) AS tz_weeks) AS tz_weeks
                    ON j.created_at >= tz_weeks.week_start AND j.created_at < tz_weeks.week_end
         LEFT JOIN contract.contract c ON c.contract_number = j.contract_number
-- Only report jobs that were started by a PDP since those jobs are the only ones we care about for analytics
WHERE j.started_by = 'PDP'
  and j.contract_number != 'UNKNOWN';

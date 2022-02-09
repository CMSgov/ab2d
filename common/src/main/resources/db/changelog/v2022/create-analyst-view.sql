-- This view excludes all information on part d enrollment completely

-- Job view gives high level information concerning a job including contract_number and organization
-- but excludes any information about Okta client creds or internal database ids.
CREATE OR REPLACE VIEW job_view AS
SELECT j.id, j.job_uuid, j.created_at, j.completed_at, j.expires_at, j.resource_types, j.status, j.request_url,
       j.output_format, j.since, j.fhir_version, tz_weeks.year_week, tz_weeks.week_start, tz_weeks.week_end, u.organization, c.contract_number, c.contract_name, c.contract_type
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
         LEFT JOIN user_account u ON j.user_account_id=u.id
-- Only report jobs that were started by a PDP since those jobs are the only ones we care about for analytics
WHERE j.started_by = 'PDP' and j.contract_number != 'UNKNOWN';

-- Limit information available about a contract
CREATE OR REPLACE VIEW contract_view AS
SELECT con.contract_number, con.contract_name, con.attested_on, con.created, con.modified, con.hpms_parent_org_name,
       con.hpms_org_marketing_name, con.update_mode, con.contract_type, u.enabled
FROM contract con
         LEFT JOIN user_account u ON con.id = u.contract_id;

CREATE OR REPLACE VIEW bcp_view AS
SELECT bcp.id AS bcp_id, con.contract_number, bcp.month, bcp.year, bcp.status, bcp.last_successful_job
FROM bene_coverage_period bcp
         INNER JOIN contract con ON bcp.contract_number = con.contract_number;

-- Excludes current_mbi and historic_mbis field
-- ANY CHANGES MADE TO THIS VIEW MUST BE DOCUMENTED
CREATE OR REPLACE VIEW coverage_view AS
SELECT contract, year, month, beneficiary_id, bene_coverage_period_id, bene_coverage_search_event_id
FROM coverage cov;

-- Create table to record statistics in
CREATE TABLE IF NOT EXISTS ab2d_statistics(
                                              statistic_name VARCHAR UNIQUE NOT NULL,
                                              statistic_value NUMERIC NOT NULL
);

INSERT INTO ab2d_statistics(statistic_name, statistic_value)
VALUES ('total_benes_served', 0);

-- Create function for calculating total benes served by the AB2D API
CREATE OR REPLACE PROCEDURE find_benes_served()
    LANGUAGE plpgsql
AS
$$
DECLARE
    con_number VARCHAR(5);
ubc BIGINT;
last_run TIMESTAMP WITH TIME ZONE;
BEGIN

-- Table to store unique benes in
CREATE TEMP TABLE benes_served(
    beneficiary_id BIGINT PRIMARY KEY NOT NULL
);

-- For each contract that has ever run a job in prod
-- find all unique benes ever queried
FOR con_number IN (SELECT DISTINCT(contract_number)
                       FROM job_view
                       WHERE contract_number LIKE 'S%' OR contract_number LIKE 'E%'
                       LIMIT 1000
    )
    LOOP

        ubc := (SELECT COUNT(*) FROM benes_served);
RAISE NOTICE 'Inserting benes for %', con_number;
RAISE NOTICE 'Benes before %', ubc;

        -- Get most recent run pulling data for the contract
last_run := (
            SELECT created_at
            FROM job_view WHERE status = 'SUCCESSFUL' AND contract_number = con_number
            ORDER BY created_at DESC
            LIMIT 1
        );
RAISE NOTICE 'Last run %', last_run;

        -- Use most recent job to determine what months of enrollment for the contract have been queried.
        -- Once those months are known, use the temp table to deduplicate the enrollment against other contracts
INSERT INTO benes_served(beneficiary_id)
SELECT DISTINCT(beneficiary_id)
FROM coverage_view
WHERE contract = con_number AND year IN (2020, 2021, 2022, 2023)
  AND bene_coverage_search_event_id IN (
    -- Find all search events with coverage (should be one per month)
    SELECT bene_coverage_search_event_id
    FROM (
             SELECT coverage_view.year, coverage_view.month, coverage_view.bene_coverage_search_event_id
             FROM coverage_view
                      INNER JOIN bcp_view ON coverage_view.bene_coverage_period_id = bcp_view.bcp_id
             WHERE bcp_view.status = 'SUCCESSFUL' AND coverage_view.contract = con_number
               AND bcp_view.year IN (2020, 2021, 2022, 2023)
             GROUP BY coverage_view.contract, coverage_view.year, coverage_view.month, coverage_view.bene_coverage_search_event_id
             ORDER BY coverage_view.contract, coverage_view.year, coverage_view.month
         ) coverage_meta
    WHERE make_timestamptz(coverage_meta.year, coverage_meta.month, 1, 0, 0, 0, 'America/New_York') < last_run
)
ON CONFLICT (beneficiary_id) DO NOTHING;

END LOOP;

ubc := (SELECT COUNT(*) FROM benes_served);

DROP TABLE IF EXISTS benes_served;

UPDATE ab2d_statistics
SET statistic_value = ubc
WHERE statistic_name = 'total_benes_served';
END
$$;

------------------------------
-- Grants to ab2d_analyst user
------------------------------

CREATE ROLE ab2d_analyst noinherit;

-- Grant read only privileges to a subset of tables in the database
GRANT SELECT ON event_api_response TO ab2d_analyst;
GRANT SELECT ON event_api_request TO ab2d_analyst;
GRANT SELECT ON event_bene_coverage_search_status_change TO ab2d_analyst;
GRANT SELECT ON event_bene_reload TO ab2d_analyst;
GRANT SELECT ON event_bene_search TO ab2d_analyst;
GRANT SELECT ON event_error TO ab2d_analyst;
GRANT SELECT ON event_file TO ab2d_analyst;
GRANT SELECT ON event_job_status_change TO ab2d_analyst;
GRANT SELECT ON job_output TO ab2d_analyst;
GRANT SELECT ON contract_view TO ab2d_analyst;
GRANT SELECT ON job_view TO ab2d_analyst;
GRANT SELECT ON bcp_view TO ab2d_analyst;
GRANT SELECT ON coverage_view TO ab2d_analyst;

GRANT SELECT, UPDATE, INSERT ON ab2d_statistics TO ab2d_analyst;
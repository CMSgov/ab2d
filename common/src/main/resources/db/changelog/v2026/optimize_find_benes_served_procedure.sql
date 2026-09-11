CREATE OR REPLACE PROCEDURE ab2d.find_benes_served()
    LANGUAGE plpgsql
AS
$$
DECLARE
    ubc BIGINT;
    max_year INT := date_part('year', current_timestamp)::int;
BEGIN

WITH contract_last_run AS MATERIALIZED (
    SELECT contract_number, MAX(created_at) AS last_run
    FROM ab2d.job_view
    WHERE status = 'SUCCESSFUL'
      AND (contract_number LIKE 'S%' OR contract_number LIKE 'E%')
    GROUP BY contract_number
),
benes_served AS (
    SELECT DISTINCT cov.beneficiary_id
    FROM ab2d.coverage_view cov
             INNER JOIN ab2d.bcp_view bcp ON bcp.bcp_id = cov.bene_coverage_period_id
             INNER JOIN contract_last_run clr ON clr.contract_number = cov.contract
    WHERE bcp.status = 'SUCCESSFUL'
      AND cov.year BETWEEN 2020 AND max_year
      AND bcp.year BETWEEN 2020 AND max_year
      AND make_timestamptz(cov.year, cov.month, 1, 0, 0, 0, 'America/New_York') < clr.last_run
)
SELECT COUNT(*) INTO ubc FROM benes_served;

RAISE NOTICE 'Total unique benes served: %', ubc;

IF ubc IS NULL OR ubc = 0 THEN
    RAISE EXCEPTION 'find_benes_served() computed % unique benes; refusing to overwrite total_benes_served', ubc;
END IF;

UPDATE ab2d.ab2d_statistics
SET statistic_value = ubc
WHERE statistic_name = 'total_benes_served';
END
$$;

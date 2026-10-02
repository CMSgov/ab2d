-- Resume of restore-2026-07.sql after the 2026-10-01 run (Actions run 36908812593) died at 5h09m
-- with "The self-hosted runner lost communication with the server". Throwaway, like its parent.
--
-- State found afterwards (read by hand in prod, 2026-10-02 ~00:05 UTC):
--   * v3.coverage_v3_restore_20260930 survived: July S4802 8,847,418 / S5884 3,815,643 /
--     S5921 2,344,653.
--   * STEP 3 committed: historical July S4802 8,847,418 / S5884 3,815,643 / S5921 2,513,725
--     (2,372,843 already there + the 140,882 the 10-01 rollback discarded).
--   * STEP 4 committed for S4802 (its 2026-07 coverage-period row exists).
--   * S5884 was mid-rebuild with no client. Postgres rolls that back when it finishes.
--   * S5921 can't be told apart: it had a 2026-07 period row from earlier archives, and its
--     summary must now include the 140,882 new rows. Rebuilt again here, which is idempotent.
--
-- So this does only the S5884 and S5921 summary rebuilds, the per-contract verification, and the
-- cleanup. It skips the S3 import and the July insert. It also drops the parent's "any other
-- contract" check, a 25M-row anti-join that could be what pushed the parent past the runner's
-- limit. The worker logs already show every other contract archived successfully on 10-01.
--
-- Before running: make sure no orphaned restore session is still running
-- (pg_stat_activity, application_name = 'psql'), or the S5884 DELETE below will wait on its locks.

\timing on
SET statement_timeout = 0;

DO $$
BEGIN
    IF to_regclass('v3.coverage_v3_restore_20260930') IS NULL THEN
        RAISE EXCEPTION 'v3.coverage_v3_restore_20260930 is gone; run restore-2026-07.sql instead';
    END IF;
END $$;

--------------------------------------------------------------------------------------------------
-- Summary rebuilds (identical to restore-2026-07.sql STEP 4), one transaction per contract.
--------------------------------------------------------------------------------------------------

BEGIN;
SET LOCAL work_mem = '512MB';
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S5884';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S5884'
GROUP BY contract, patient_id, current_mbi;
WITH
coverage_periods_unparsed AS (
    SELECT DISTINCT json_array_elements(to_json(historical_coverage_summaries))::TEXT AS text
    FROM v3.coverage_v3_history_summary
    WHERE contract = 'S5884'
),
coverage_periods_parsed AS (
    SELECT 'S5884' AS contract,
           split_part(translate(text, '[]', ''), ',', 1)::INT AS year,
           split_part(translate(text, '[]', ''), ',', 2)::INT AS month
    FROM coverage_periods_unparsed
)
INSERT INTO v3.coverage_v3_history_summary_coverage_periods
SELECT * FROM coverage_periods_parsed
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

BEGIN;
SET LOCAL work_mem = '512MB';
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S5921';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S5921'
GROUP BY contract, patient_id, current_mbi;
WITH
coverage_periods_unparsed AS (
    SELECT DISTINCT json_array_elements(to_json(historical_coverage_summaries))::TEXT AS text
    FROM v3.coverage_v3_history_summary
    WHERE contract = 'S5921'
),
coverage_periods_parsed AS (
    SELECT 'S5921' AS contract,
           split_part(translate(text, '[]', ''), ',', 1)::INT AS year,
           split_part(translate(text, '[]', ''), ',', 2)::INT AS month
    FROM coverage_periods_unparsed
)
INSERT INTO v3.coverage_v3_history_summary_coverage_periods
SELECT * FROM coverage_periods_parsed
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

--------------------------------------------------------------------------------------------------
-- Verify (identical to restore-2026-07.sql STEP 5, minus the all-contracts scan).
--------------------------------------------------------------------------------------------------

-- Expect extract_rows_missing_from_historical = 0 for all three. historical_rows can be slightly
-- above extract_rows (S5921 already held July rows from earlier archives), never below.
SELECT r.contract,
       r.extract_rows,
       (SELECT count(*) FROM v3.coverage_v3_historical h
         WHERE h.contract = r.contract AND h.year = 2026 AND h.month = 7) AS historical_rows,
       (SELECT count(*) FROM v3.coverage_v3_restore_20260930 x
         WHERE x.contract = r.contract AND x.year = 2026 AND x.month = 7
           AND NOT EXISTS (
               SELECT 1 FROM v3.coverage_v3_historical h
               WHERE h.contract = x.contract AND h.year = x.year AND h.month = x.month
                 AND h.patient_id = x.patient_id
                 AND h.current_mbi IS NOT DISTINCT FROM x.current_mbi
           )) AS extract_rows_missing_from_historical
FROM (
    SELECT contract, count(*) AS extract_rows
    FROM v3.coverage_v3_restore_20260930
    WHERE contract IN ('S4802', 'S5884', 'S5921') AND year = 2026 AND month = 7
    GROUP BY contract
) r
ORDER BY r.contract;

-- This is what CoverageV3ServiceImpl.getCoveragePeriods feeds to
-- CoverageV3CoveragePeriodsPresentCheck. Expect three rows.
SELECT contract, year, month
FROM v3.coverage_v3_history_summary_coverage_periods
WHERE contract IN ('S4802', 'S5884', 'S5921')
  AND year = 2026 AND month = 7
ORDER BY contract;

-- July must be reachable through the summary arrays too, or exports still miss it. Sampled at 100
-- benes per contract; expect 100 for each.
--
-- NOTE: do not write this as historical_coverage_summaries @> ARRAY[ARRAY[2026,7]]. Postgres array
-- containment ignores dimensions and compares flattened elements, so an array holding
-- [[2026,8],[2025,7]] would satisfy it. The pairs have to be parsed, exactly as
-- POPULATE_HISTORY_SUMMARY_COVERAGE_PERIODS_FOR_CONTRACT does.
WITH sample AS (
    SELECT c.contract, j.patient_id, j.current_mbi
    FROM (VALUES ('S4802'), ('S5884'), ('S5921')) AS c(contract)
    CROSS JOIN LATERAL (
        SELECT DISTINCT h.patient_id, h.current_mbi
        FROM v3.coverage_v3_historical h
        WHERE h.contract = c.contract AND h.year = 2026 AND h.month = 7
        LIMIT 100
    ) j
)
SELECT s.contract, count(*) AS sampled_benes_with_july
FROM sample sm
JOIN v3.coverage_v3_history_summary s
  ON s.contract = sm.contract
 AND s.patient_id = sm.patient_id
 AND s.current_mbi IS NOT DISTINCT FROM sm.current_mbi
WHERE EXISTS (
    SELECT 1
    FROM json_array_elements(to_json(s.historical_coverage_summaries)) AS e(pair)
    WHERE split_part(translate(e.pair::text, '[]', ''), ',', 1)::int = 2026
      AND split_part(translate(e.pair::text, '[]', ''), ',', 2)::int = 7
)
GROUP BY s.contract
ORDER BY s.contract;

--------------------------------------------------------------------------------------------------
-- Clean up.
--------------------------------------------------------------------------------------------------

DROP TABLE v3.coverage_v3_restore_20260930;

-- One-off restore of 2026-07 enrollment for S4802, S5884, S5921 (ab2d-east-prod).
-- Throwaway: delete this file once the V3 coverage check is green.
--
-- Run it with the restore-2026-07 workflow (push to the backfill-july-2026 branch, or Actions ->
-- restore-2026-07 -> Run workflow, type "restore"). That is the only route to the prod database: it
-- runs on a CodeBuild runner inside the VPC, and handles the S3 step below for you. The shell
-- commands here are for running by hand.
--
-- Source is the 2026-09-30 IDR extract, still in the importer bucket as a noncurrent version.
-- The importer pulls DATE_TRUNC('month', CURRENT_DATE) and the two months before it
-- (SnowflakeCoverageQueryService, GENERATOR(ROWCOUNT => 3)), so the 09-30 extract carries
-- 2026-07/08/09 and is the LAST extract that contains July. The 10-01 extract carries 08/09/10.
--
--------------------------------------------------------------------------------------------------
-- WHY JULY WAS LOST -- a third, different cause (May: statement_timeout; June: BFD-sync skip).
--
-- The 06:30 moveToHistorical run on 2026-10-01 did start and did copy July, but the copy, the
-- history summary rebuild and the old-month delete all run in ONE transaction,
-- @Transactional(timeout=3600). For the three largest movers the summary rebuild pushed it past an
-- hour, the transaction timed out, and the whole thing -- including the copy -- rolled back:
--
--   S4802  06:59:12 start, 07:22:48 "Moved 8847418 rows", 07:59:12 deadline -> rolled back
--   S5884  11:37:34 start, 11:48:37 "Moved 3815643 rows", 12:37:34 deadline -> rolled back
--   S5921  12:42:50 start, 12:46:45 "Moved 140882 rows",  13:42:50 deadline -> rolled back
--
--   ("Transaction timed out: deadline was ...", surfacing from the audit write in the catch around
--   populateHistorySummaryForContract.)
--
-- July then sat only in v3.coverage_v3, and the prod staging copy (still the old unconditional
-- DELETE FROM v3.coverage_v3 WHERE contract = ?) replaced it with the 10-01 extract, which has no
-- July: S4802 at 12:08, S5921 at 13:20, S5884 at 13:09.
--
-- S5921 is only partly missing: historical already held 2,372,843 July rows, and only the 140,882
-- rows the rollback discarded are gone. ON CONFLICT DO NOTHING below makes including it safe.
--------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------
-- STEP 1 (shell, before running this file): make the September 30 object current again.
--
-- The importer deletes the CSV after each run, so the key has a delete marker on top and
-- aws_s3.table_import_from_s3 (which reads the current version) cannot see it. Removing a delete
-- marker is s3:DeleteObjectVersion, which the bucket's DenyObjectAccessExceptIDRDBImporterRoles
-- statement does not deny -- unlike GetObject/PutObject, so a copy-object would fail here.
--
--   BKT=ab2d-prod-idr-db-importer-20260306203002209800000001
--   aws s3api delete-object --bucket "$BKT" \
--     --key coverage_v3_20260930.csv \
--     --version-id HcUOUHMUS2niIXn79a7ZDVQrJfZa3OhF
--
--   # confirm it is current again. Verify with list-object-versions, NOT head-object:
--   # HeadObject is s3:GetObject, which the bucket policy denies to everyone except the two
--   # importer roles, so head-object returns 403 even though the removal succeeded.
--   aws s3api list-object-versions --bucket "$BKT" --prefix coverage_v3_20260930.csv \
--     --query "Versions[?Key=='coverage_v3_20260930.csv' && IsLatest].[VersionId,Size]"
--
-- Object version being restored: lbCbu3ulUmtsCPvYvKK.2WimpaDQvKsJ, 2026-09-30T11:04:26Z,
-- 2639663842 bytes. Expect that exact size back. The bucket lifecycle only transitions noncurrent
-- versions to STANDARD_IA after 30 days and never expires them, so this is not on a clock.
--------------------------------------------------------------------------------------------------

\timing on

-- The 20 minute cap (statement_timeout, 1200000 ms, ops/services/10-core/database.tf) would kill
-- the import and the S4802 summary rebuild. Session scope only.
SET statement_timeout = 0;

--------------------------------------------------------------------------------------------------
-- STEP 2: land the extract in a scratch table.
--
-- Deliberately NOT v3.coverage_v3_staging: the hourly copyFromStagingTablesToRecentForAllContracts()
-- job drains that table by deleting every row for a contract in v3.coverage_v3 and replacing it
-- with staging. Loading a 3 month extract there would replace live coverage for every contract.
--------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS v3.coverage_v3_restore_20260930;

CREATE UNLOGGED TABLE v3.coverage_v3_restore_20260930 (
    patient_id  BIGINT NOT NULL,
    contract    VARCHAR(15) NOT NULL,
    year        INT NOT NULL,
    month       INT NOT NULL,
    current_mbi VARCHAR(32)
);

SELECT aws_s3.table_import_from_s3(
    'v3.coverage_v3_restore_20260930',
    'patient_id,contract,year,month,current_mbi',
    '(format csv, null ''NULL'')',
    aws_commons.create_s3_uri(
        'ab2d-prod-idr-db-importer-20260306203002209800000001',
        'coverage_v3_20260930.csv',
        'us-east-1'
    )
);

-- Sanity check before touching anything real. Expect 2026-07, 2026-08 and 2026-09, and a non-zero
-- 2026-07 count for all three contracts. From the 2026-09-30 worker log the three month staging
-- totals were S4802 26,692,342 / S5884 11,532,954 / S5921 7,012,720, so July should land near a
-- third of each (the 10-01 move attempts logged 8,847,418 / 3,815,643 for S4802 / S5884).
-- The restore aborts below if any contract has no July rows.
SELECT contract, year, month, count(*)
FROM v3.coverage_v3_restore_20260930
WHERE contract IN ('S4802', 'S5884', 'S5921')
GROUP BY contract, year, month
ORDER BY contract, year, month;

DO $$
DECLARE
    missing TEXT;
BEGIN
    SELECT string_agg(c.contract, ', ') INTO missing
    FROM (VALUES ('S4802'), ('S5884'), ('S5921')) AS c(contract)
    WHERE NOT EXISTS (
        SELECT 1 FROM v3.coverage_v3_restore_20260930 r
        WHERE r.contract = c.contract AND r.year = 2026 AND r.month = 7
    );
    IF missing IS NOT NULL THEN
        RAISE EXCEPTION 'No 2026-07 rows in the restored extract for: %', missing;
    END IF;
END $$;

--------------------------------------------------------------------------------------------------
-- STEP 3: insert July only, for the three contracts only.
--
-- Restricting to 2026-07 matters: August and September are still inside the rolling import window
-- and live in v3.coverage_v3. Putting them in historical early would leave moveToHistorical with
-- nothing to move next month.
--
-- July belongs in historical, not recent: the cutoff is now 2026-08-01, so anything written to
-- v3.coverage_v3 for July would be deleted again by the next staging copy.
--------------------------------------------------------------------------------------------------

BEGIN;

-- ON CONFLICT alone is not enough: the unique key includes current_mbi, which is nullable, and
-- NULLs never conflict, so a re-run would duplicate every NULL-MBI row. The NOT EXISTS with
-- IS NOT DISTINCT FROM makes the insert idempotent; ON CONFLICT stays as a backstop.
INSERT INTO v3.coverage_v3_historical (patient_id, contract, year, month, current_mbi)
SELECT DISTINCT r.patient_id, r.contract, r.year, r.month, r.current_mbi
FROM v3.coverage_v3_restore_20260930 r
WHERE r.year = 2026
  AND r.month = 7
  AND r.contract IN ('S4802', 'S5884', 'S5921')
  AND NOT EXISTS (
      SELECT 1 FROM v3.coverage_v3_historical h
      WHERE h.contract = r.contract AND h.year = r.year AND h.month = r.month
        AND h.patient_id = r.patient_id
        AND h.current_mbi IS NOT DISTINCT FROM r.current_mbi
  )
ON CONFLICT (patient_id, contract, year, month, current_mbi) DO NOTHING;

COMMIT;

--------------------------------------------------------------------------------------------------
-- STEP 4: rebuild the summary tables for the three contracts.
--
-- Required, not cosmetic. CoverageV3ServiceImpl.getCoveragePeriods -- which is what the alerting
-- check reads -- unions v3.coverage_v3_history_summary_coverage_periods with v3.coverage_v3, and
-- GetAggregatedCoverageMembership builds export membership from v3.coverage_v3_history_summary.
--
-- Same statements moveToHistorical runs today (DELETE/INSERT_HISTORY_SUMMARY_FOR_CONTRACT with
-- work_mem 512MB, then POPULATE_HISTORY_SUMMARY_COVERAGE_PERIODS_FOR_CONTRACT), one contract per
-- transaction so a slow one cannot discard the others. Each block is idempotent (full delete +
-- rebuild from historical), so re-running the file is safe.
--------------------------------------------------------------------------------------------------

BEGIN;
SET LOCAL work_mem = '512MB';
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S4802';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S4802'
GROUP BY contract, patient_id, current_mbi;
WITH
coverage_periods_unparsed AS (
    SELECT DISTINCT json_array_elements(to_json(historical_coverage_summaries))::TEXT AS text
    FROM v3.coverage_v3_history_summary
    WHERE contract = 'S4802'
),
coverage_periods_parsed AS (
    SELECT 'S4802' AS contract,
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
-- STEP 5: verify.
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

-- Any OTHER contract whose July is in the extract but not in historical. Expect zero rows; if this
-- returns anything, July was lost there too and the contract needs adding to STEP 3/4.
SELECT x.contract, count(*) AS july_rows_missing_from_historical
FROM v3.coverage_v3_restore_20260930 x
WHERE x.year = 2026 AND x.month = 7
  AND x.contract NOT IN ('S4802', 'S5884', 'S5921')
  AND NOT EXISTS (
      SELECT 1 FROM v3.coverage_v3_historical h
      WHERE h.contract = x.contract AND h.year = x.year AND h.month = x.month
        AND h.patient_id = x.patient_id
        AND h.current_mbi IS NOT DISTINCT FROM x.current_mbi
  )
GROUP BY x.contract
ORDER BY x.contract;

--------------------------------------------------------------------------------------------------
-- STEP 6: clean up.
--------------------------------------------------------------------------------------------------

DROP TABLE v3.coverage_v3_restore_20260930;

-- The CSV cannot be re-hidden. Re-adding a delete marker is s3:DeleteObject, which the bucket
-- policy denies to every principal except ab2d-prod-idr-db-importer-task-role and
-- ab2d-prod-database-import-s3. So coverage_v3_20260930.csv stays the current version of its key.
-- That is harmless: the object was already in the bucket as a noncurrent version readable by the
-- same two roles, and the importer writes a new date-stamped key each day.
--
-- The V3 coverage check runs at 00:00 and 12:00 UTC. The alert should stop on the next run.

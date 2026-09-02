-- One-off restore of 2026-06 enrollment for S4802, S5601, S5805, S5820, S5884, S9701
-- (ab2d-east-prod). Throwaway: delete this file once the V3 coverage check is green.
--
-- Run it with the restore-2026-06 workflow (Actions -> restore-2026-06 -> Run workflow, type
-- "restore"). That is the only route to the prod database: it runs on a CodeBuild runner inside the
-- VPC, and handles the S3 step below for you. The shell commands here are for running by hand.
--
-- Source is the 2026-08-31 IDR extract, still in the importer bucket as a noncurrent version.
-- The importer pulls DATE_TRUNC('month', CURRENT_DATE) and the two months before it
-- (SnowflakeCoverageQueryService, GENERATOR(ROWCOUNT => 3)), so the 08-31 extract carries
-- 2026-06/07/08 and is the LAST extract that contains June. The 09-01 extract carries 07/08/09.
--
--------------------------------------------------------------------------------------------------
-- WHY JUNE WAS LOST -- this is NOT the same failure as restore-2026-05.sql.
--
-- May was lost to a statement_timeout rollback. June was lost to a scheduling race, and the worker
-- logged no error at all:
--
--   2026-08-31 06:30, 18:30  moveToHistorical(S4802) -> 0 rows. Correct: the cutoff is
--                            date_trunc('month', CURRENT_DATE) - 2 months, so on 08-31 that is
--                            2026-06-01 and June is not yet eligible.
--   2026-09-01 05:00-15:00   "BFD coverage sync is in progress" -- this guard sits on BOTH
--                            copyFromStagingTablesToRecent AND moveToHistoricalForAllContracts,
--                            so every hourly staging copy and the 06:30 historical run were
--                            skipped. 06:30 was the only historical run inside June's window.
--   2026-09-01 16:02         BFD sync clears. The hourly staging copy runs FIRST, deletes all
--                            26,547,805 rows for S4802 from v3.coverage_v3 and replaces them with
--                            26,696,271 rows from the 09-01 extract -- which has no June.
--   2026-09-01 18:58         moveToHistorical(S4802) -> 0 rows. Nothing left to move. Silent.
--   2026-09-02 00:00         CoverageV3CoveragePeriodsPresentCheck: "S4802-2026-6 no enrollment".
--
-- Month N-2 is only eligible for archiving between 00:00 on the 1st and that day's first staging
-- copy after the 11:00 UTC import. moveToHistorical runs at 06:30 and 18:30, so exactly one
-- scheduled run falls inside that window, once a month, and anything that skips it loses the month
-- permanently. There is no safety buffer.
--------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------
-- STEP 1 (shell, before running this file): make the August 31 object current again.
--
-- The importer deletes the CSV after each run, so the key has a delete marker on top and
-- aws_s3.table_import_from_s3 (which reads the current version) cannot see it. Removing a delete
-- marker is s3:DeleteObjectVersion, which the bucket's DenyObjectAccessExceptIDRDBImporterRoles
-- statement does not deny -- unlike GetObject/PutObject, so a copy-object would fail here.
--
--   BKT=ab2d-prod-idr-db-importer-20260306203002209800000001
--   aws s3api delete-object --bucket "$BKT" \
--     --key coverage_v3_20260831.csv \
--     --version-id VMAerbBSnPynMqemhC30pp3r7WeUHHtl
--
--   # confirm it is current again. Verify with list-object-versions, NOT head-object:
--   # HeadObject is s3:GetObject, which the bucket policy denies to everyone except the two
--   # importer roles, so head-object returns 403 even though the removal succeeded.
--   aws s3api list-object-versions --bucket "$BKT" --prefix coverage_v3_20260831.csv \
--     --query "Versions[?Key=='coverage_v3_20260831.csv' && IsLatest].[VersionId,Size]"
--
-- Object version being restored: nrtxtEj6aDoeQ6GYqzAdvlSDnoNQAtur, 2026-08-31T11:03:28Z,
-- 2631965837 bytes. Expect that exact size back.
--------------------------------------------------------------------------------------------------

\timing on

-- The 20 minute cap (statement_timeout, 1200000 ms, ops/services/10-core/database.tf) is what
-- destroyed May. The import and the summary rebuilds both run long. Session scope only.
SET statement_timeout = 0;

--------------------------------------------------------------------------------------------------
-- STEP 2: land the extract in a scratch table.
--
-- Deliberately NOT v3.coverage_v3_staging: the hourly copyFromStagingTablesToRecentForAllContracts()
-- job drains that table by deleting every row for a contract in v3.coverage_v3 and replacing it
-- with staging. Loading a 3 month extract there would replace live coverage for every contract.
--------------------------------------------------------------------------------------------------

DROP TABLE IF EXISTS v3.coverage_v3_restore_20260831;

CREATE UNLOGGED TABLE v3.coverage_v3_restore_20260831 (
    patient_id  BIGINT NOT NULL,
    contract    VARCHAR(15) NOT NULL,
    year        INT NOT NULL,
    month       INT NOT NULL,
    current_mbi VARCHAR(32)
);

SELECT aws_s3.table_import_from_s3(
    'v3.coverage_v3_restore_20260831',
    'patient_id,contract,year,month,current_mbi',
    '(format csv, null ''NULL'')',
    aws_commons.create_s3_uri(
        'ab2d-prod-idr-db-importer-20260306203002209800000001',
        'coverage_v3_20260831.csv',
        'us-east-1'
    )
);

-- Sanity check before touching anything real. Expect 2026-06, 2026-07 and 2026-08, and a non-zero
-- 2026-06 count for all six contracts. From the 2026-08-31 worker log the three month staging
-- totals were S4802 26,547,805 / S5601 11,594,964 / S5805 2,831 / S5820 1,071,098 /
-- S5884 11,447,766 / S9701 305,593, so June should land near a third of each.
-- STOP HERE if 2026-06 is absent or any contract is missing.
SELECT contract, year, month, count(*)
FROM v3.coverage_v3_restore_20260831
WHERE contract IN ('S4802', 'S5601', 'S5805', 'S5820', 'S5884', 'S9701')
GROUP BY contract, year, month
ORDER BY contract, year, month;

--------------------------------------------------------------------------------------------------
-- STEP 3: insert June only, for the six contracts only.
--
-- Restricting to 2026-06 matters: July and August are still inside the rolling import window and
-- live in v3.coverage_v3. Putting them in historical early would leave moveToHistorical with
-- nothing to move, so it would return early on rowsMoved == 0 and never run
-- deleteMonthsOldCoverage, stranding those rows in the recent table permanently.
--
-- June belongs in historical, not recent: the cutoff is now 2026-07-01, so anything written to
-- v3.coverage_v3 for June would be deleted again by the next deleteMonthsOldCoverage or staging
-- copy.
--------------------------------------------------------------------------------------------------

BEGIN;

INSERT INTO v3.coverage_v3_historical (patient_id, contract, year, month, current_mbi)
SELECT patient_id, contract, year, month, current_mbi
FROM v3.coverage_v3_restore_20260831
WHERE year = 2026
  AND month = 6
  AND contract IN ('S4802', 'S5601', 'S5805', 'S5820', 'S5884', 'S9701')
ON CONFLICT (patient_id, contract, year, month, current_mbi) DO NOTHING;

COMMIT;

--------------------------------------------------------------------------------------------------
-- STEP 4: rebuild the summary tables for the six contracts.
--
-- Required, not cosmetic. CoverageV3ServiceImpl.getCoveragePeriods -- which is what the alerting
-- check reads -- unions v3.coverage_v3_history_summary_coverage_periods with v3.coverage_v3, and
-- GetAggregatedCoverageMembership builds export membership from v3.coverage_v3_history_summary.
-- Skipping this clears nothing and leaves V3 exports still missing June.
--
-- Same statements moveToHistorical runs, one contract per transaction so a slow one cannot discard
-- the others -- that is exactly how the May restore failed halfway and needed resume-2026-05.sql.
-- Each block is idempotent (full delete + rebuild from historical), so re-running the file is safe
-- and you can comment out the contracts that already finished.
--------------------------------------------------------------------------------------------------

BEGIN;
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S4802';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S4802'
GROUP BY contract, patient_id, current_mbi;
INSERT INTO v3.coverage_v3_history_summary_coverage_periods (contract, year, month)
SELECT DISTINCT contract, year, month FROM v3.coverage_v3_historical WHERE contract = 'S4802'
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

BEGIN;
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S5601';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S5601'
GROUP BY contract, patient_id, current_mbi;
INSERT INTO v3.coverage_v3_history_summary_coverage_periods (contract, year, month)
SELECT DISTINCT contract, year, month FROM v3.coverage_v3_historical WHERE contract = 'S5601'
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

BEGIN;
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S5805';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S5805'
GROUP BY contract, patient_id, current_mbi;
INSERT INTO v3.coverage_v3_history_summary_coverage_periods (contract, year, month)
SELECT DISTINCT contract, year, month FROM v3.coverage_v3_historical WHERE contract = 'S5805'
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

BEGIN;
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S5820';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S5820'
GROUP BY contract, patient_id, current_mbi;
INSERT INTO v3.coverage_v3_history_summary_coverage_periods (contract, year, month)
SELECT DISTINCT contract, year, month FROM v3.coverage_v3_historical WHERE contract = 'S5820'
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

BEGIN;
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S5884';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S5884'
GROUP BY contract, patient_id, current_mbi;
INSERT INTO v3.coverage_v3_history_summary_coverage_periods (contract, year, month)
SELECT DISTINCT contract, year, month FROM v3.coverage_v3_historical WHERE contract = 'S5884'
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

BEGIN;
DELETE FROM v3.coverage_v3_history_summary WHERE contract = 'S9701';
INSERT INTO v3.coverage_v3_history_summary
    (contract, patient_id, current_mbi, historical_coverage_summaries)
SELECT contract, patient_id, current_mbi,
       array_agg(array[year, month] ORDER BY year ASC, month ASC)
FROM v3.coverage_v3_historical
WHERE contract = 'S9701'
GROUP BY contract, patient_id, current_mbi;
INSERT INTO v3.coverage_v3_history_summary_coverage_periods (contract, year, month)
SELECT DISTINCT contract, year, month FROM v3.coverage_v3_historical WHERE contract = 'S9701'
ON CONFLICT (contract, year, month) DO NOTHING;
COMMIT;

--------------------------------------------------------------------------------------------------
-- STEP 5: verify. The second query is the one that matters -- it is what
-- CoverageV3ServiceImpl.getCoveragePeriods feeds to CoverageV3CoveragePeriodsPresentCheck.
-- Expect six rows from each.
--------------------------------------------------------------------------------------------------

SELECT contract, year, month, count(*) AS historical_rows
FROM v3.coverage_v3_historical
WHERE contract IN ('S4802', 'S5601', 'S5805', 'S5820', 'S5884', 'S9701')
  AND year = 2026 AND month = 6
GROUP BY contract, year, month
ORDER BY contract;

SELECT contract, year, month
FROM v3.coverage_v3_history_summary_coverage_periods
WHERE contract IN ('S4802', 'S5601', 'S5805', 'S5820', 'S5884', 'S9701')
  AND year = 2026 AND month = 6
ORDER BY contract;

-- June must be reachable through the summary arrays too, or exports still miss it.
-- Sampled at 100 benes per contract: the arrays are wide and the tables are tens of millions of
-- rows, and the rebuild above is a deterministic function of historical, so a sample is enough to
-- prove the arrays actually carry the pair. Expect 100 for each contract (or the June bene count
-- for S5805, which is tiny).
--
-- NOTE: do not be tempted to write this as historical_coverage_summaries @> ARRAY[ARRAY[2026,6]].
-- Postgres array containment ignores dimensions and compares flattened elements, so an array
-- holding [[2026,7],[2025,6]] would satisfy it. The pairs have to be parsed. This uses exactly the
-- json/split_part parsing from POPULATE_HISTORY_SUMMARY_COVERAGE_PERIODS in
-- CoverageV3SyncServiceImpl, so it agrees with the app by construction.
WITH sample AS (
    SELECT c.contract, j.patient_id, j.current_mbi
    FROM (VALUES ('S4802'), ('S5601'), ('S5805'), ('S5820'), ('S5884'), ('S9701')) AS c(contract)
    CROSS JOIN LATERAL (
        SELECT DISTINCT h.patient_id, h.current_mbi
        FROM v3.coverage_v3_historical h
        WHERE h.contract = c.contract AND h.year = 2026 AND h.month = 6
        LIMIT 100
    ) j
)
SELECT s.contract, count(*) AS sampled_benes_with_june
FROM sample sm
JOIN v3.coverage_v3_history_summary s
  ON s.contract = sm.contract
 AND s.patient_id = sm.patient_id
 AND s.current_mbi IS NOT DISTINCT FROM sm.current_mbi
WHERE EXISTS (
    SELECT 1
    FROM json_array_elements(to_json(s.historical_coverage_summaries)) AS e(pair)
    WHERE split_part(translate(e.pair::text, '[]', ''), ',', 1)::int = 2026
      AND split_part(translate(e.pair::text, '[]', ''), ',', 2)::int = 6
)
GROUP BY s.contract
ORDER BY s.contract;

--------------------------------------------------------------------------------------------------
-- STEP 6: clean up.
--------------------------------------------------------------------------------------------------

DROP TABLE v3.coverage_v3_restore_20260831;

-- The CSV cannot be re-hidden. Re-adding a delete marker is s3:DeleteObject, which the bucket
-- policy denies to every principal except ab2d-prod-idr-db-importer-task-role and
-- ab2d-prod-database-import-s3 -- neither of which a human or the GitHub Actions role can assume.
-- So coverage_v3_20260831.csv stays the current version of its key.
--
-- That is harmless: the object was already in the bucket as a noncurrent version, readable by
-- exactly the same two roles via GetObjectVersion. Making it current changes no access control,
-- and the importer writes a new date-stamped key each day, so nothing collides.
--
-- The V3 coverage check runs at 00:00 and 12:00 UTC. The alert should stop on the next run.

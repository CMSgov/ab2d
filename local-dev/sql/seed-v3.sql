-- Seed synthetic V3 beneficiaries for contract Z0001.
--
-- injects multi-mbi and opt-out patients as well

\if :{?benes}
\else
  \set benes 25000
\endif

BEGIN;

-- Single source of truth for this run's benes + their edge-case classification.
DROP TABLE IF EXISTS _seed_bene;
CREATE TEMP TABLE _seed_bene ON COMMIT DROP AS
SELECT gs                                        AS patient_id,
       'V3MBI' || LPAD(gs::text, 9, '0')         AS primary_mbi,
       'V3MBI' || LPAD(gs::text, 9, '0') || 'B'  AS second_mbi,
       (gs % 25 = 0)                             AS has_second,
       (gs % 10 <> 0)                            AS primary_shares,
       (gs % 75 <> 0)                            AS second_shares
FROM generate_series(1, :benes) AS gs;

-- Primary attribution row for every bene.
INSERT INTO v3.coverage_v3 (patient_id, contract, year, month, current_mbi)
SELECT patient_id, 'Z0001', 2023, 1, primary_mbi
FROM _seed_bene
ON CONFLICT DO NOTHING;

-- Multi-mbi benes
INSERT INTO v3.coverage_v3 (patient_id, contract, year, month, current_mbi)
SELECT patient_id, 'Z0001', 2023, 1, second_mbi
FROM _seed_bene
WHERE has_second
ON CONFLICT DO NOTHING;


INSERT INTO current_mbi (mbi, effective_date, share_data)
SELECT primary_mbi, NOW()::date, primary_shares
FROM _seed_bene
ON CONFLICT (mbi) DO UPDATE SET share_data = EXCLUDED.share_data;

INSERT INTO current_mbi (mbi, effective_date, share_data)
SELECT second_mbi, NOW()::date, second_shares
FROM _seed_bene
WHERE has_second
ON CONFLICT (mbi) DO UPDATE SET share_data = EXCLUDED.share_data;

COMMIT;

-- report
SELECT
    count(DISTINCT c.patient_id)                                      AS distinct_benes,
    count(*)                                                          AS coverage_rows,
    count(*) FILTER (WHERE m.share_data IS FALSE)                     AS opted_out_mbi_rows,
    count(DISTINCT c.patient_id) FILTER (WHERE c.patient_id % 25 = 0) AS multi_mbi_benes
FROM v3.coverage_v3 c
LEFT JOIN current_mbi m ON m.mbi = c.current_mbi
WHERE c.contract = 'Z0001';

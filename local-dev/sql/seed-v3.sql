-- Seed synthetic V3 beneficiaries for contract Z0001
\if :{?benes}
\else
  \set benes 2500
\endif

BEGIN;

INSERT INTO v3.coverage_v3 (patient_id, contract, year, month, current_mbi)
SELECT gs,
       'Z0001',
       2023,
       1,
       'V3MBI' || LPAD(gs::text, 9, '0')
FROM generate_series(1, :benes) AS gs
ON CONFLICT DO NOTHING;

-- opt in all the benes
INSERT INTO current_mbi (mbi, effective_date, share_data)
SELECT DISTINCT current_mbi, NOW()::date, true
FROM v3.coverage_v3
WHERE contract = 'Z0001'
ON CONFLICT (mbi) DO NOTHING;

COMMIT;

SELECT count(DISTINCT patient_id) AS v3_benes_for_z0001
FROM v3.coverage_v3
WHERE contract = 'Z0001';

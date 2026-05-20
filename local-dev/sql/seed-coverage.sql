-- Seed coverage data.
-- Apply after seed-contract.sql

BEGIN;

DO $$
DECLARE
    v_contract CONSTANT text := 'Z0001';
    v_success  CONSTANT text := 'SUCCESSFUL';
BEGIN
    -- Coverage periods
    INSERT INTO bene_coverage_period (id, contract_number, month, year, status, last_successful_job, created, modified)
    SELECT
        nextval('bene_coverage_period_seq'),
        v_contract,
        m.month,
        2023,
        v_success,
        NOW(),
        NOW(),
        NOW()
    FROM generate_series(1, 12) AS m(month)
    LEFT JOIN bene_coverage_period bcp
           ON bcp.contract_number = v_contract
          AND bcp.year = 2023
          AND bcp.month = m.month
    WHERE bcp.id IS NULL;

    -- Search events
    INSERT INTO event_bene_coverage_search_status_change (id, bene_coverage_period_id, old_status, new_status, description, created, modified)
    SELECT
        nextval('event_bene_coverage_search_status_change_seq'),
        bcp.id,
        'IN_PROGRESS',
        v_success,
        'Seeded by local-dev/sql/seed-coverage.sql',
        NOW(),
        NOW()
    FROM bene_coverage_period bcp
    LEFT JOIN event_bene_coverage_search_status_change ev
           ON ev.bene_coverage_period_id = bcp.id
          AND ev.new_status = v_success
    WHERE bcp.contract_number = v_contract
      AND bcp.year = 2023
      AND ev.id IS NULL;

    -- Coverage rows
    WITH benes(beneficiary_id, current_mbi) AS (
        VALUES
            (20140000008325::bigint, 'LOCAL_DEV_MBI_1'),
            (20140000009893::bigint, 'LOCAL_DEV_MBI_2')
    )
    INSERT INTO coverage (
        bene_coverage_period_id, bene_coverage_search_event_id, contract, year, month,
        beneficiary_id, current_mbi, historic_mbis
    )
    SELECT
        bcp.id,
        (SELECT id FROM event_bene_coverage_search_status_change ev
           WHERE ev.bene_coverage_period_id = bcp.id AND ev.new_status = v_success
           ORDER BY ev.id DESC LIMIT 1),
        v_contract,
        bcp.year,
        bcp.month,
        b.beneficiary_id,
        b.current_mbi,
        NULL
    FROM bene_coverage_period bcp
    CROSS JOIN benes b
    WHERE bcp.contract_number = v_contract AND bcp.year = 2023
    ON CONFLICT DO NOTHING;
END $$;

COMMIT;

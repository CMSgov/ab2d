CREATE SCHEMA v3;

CREATE TABLE IF NOT EXISTS v3.coverage_v3
(
    patient_id bigint NOT NULL,
    contract character varying(15)  NOT NULL,
    "year" integer NOT NULL,
    "month" integer NOT NULL,
    current_mbi character varying(32),
    CONSTRAINT coverage_v3_unique UNIQUE (patient_id, contract, "year", "month", current_mbi)
);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_staging (LIKE v3.coverage_v3);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical
(
    patient_id bigint NOT NULL,
    contract character varying(15)  NOT NULL,
    "year" integer NOT NULL,
    "month" integer NOT NULL,
    current_mbi character varying(32),
    CONSTRAINT coverage_v3_historical_unique UNIQUE (patient_id, contract, "year", "month", current_mbi)
);

CREATE TABLE IF NOT EXISTS current_mbi
(
    mbi character varying(32) NOT NULL,
    effective_date date,
    share_data boolean
);

-- 'M3' and 'M4' omitted
INSERT INTO v3.coverage_v3_historical(patient_id, contract, "year", "month", current_mbi)
VALUES
    (6, 'Z9999', 2025, 9,  'M6'),
    (6, 'Z9999', 2025, 10, 'M6'),
    (6, 'Z9999', 2025, 11, 'M6'),

    (7, 'Z1234', 2025, 9,  'M7'),
    (7, 'Z1234', 2025, 10, 'M7'),
    (7, 'Z1234', 2025, 11, 'M7'),

    (1, 'Z0000', 2025, 6,  'M1'),
    (1, 'Z0000', 2025, 7,  'M1'),
    (1, 'Z0000', 2025, 8,  'M1'),
    (1, 'Z0000', 2025, 9,  'M1'),
    (1, 'Z0000', 2025, 10, 'M1'),
    (1, 'Z0000', 2025, 11, 'M1'),

    (2, 'Z0000', 2025, 9,  'M2'),
    (2, 'Z0000', 2025, 10, 'M2'),
    (2, 'Z0000', 2025, 11, 'M2'),

    (5, 'Z8888', 2025, 7,  'M5'),
    (5, 'Z8888', 2025, 8,  'M5'),
    (5, 'Z8888', 2025, 9,  'M5')
;

-- 'M5' omitted
-- Patient 7 has MBI=M7 in historical table and MBI=X7 in recent coverage table
INSERT INTO v3.coverage_v3(patient_id, contract, "year", "month", current_mbi)
VALUES
    (1, 'Z0000', 2025, 12, 'M1'),
    (1, 'Z0000', 2026, 1,  'M1'),
    (1, 'Z0000', 2026, 2,  'M1'),

    (2, 'Z0000', 2025, 12, 'M2'),
    (2, 'Z0000', 2026, 1,  'M2'),
    (2, 'Z0000', 2026, 2,  'M2'),

    (3, 'Z0000', 2025, 12, 'M3'),
    (3, 'Z0000', 2026, 1,  'M3'),
    (3, 'Z0000', 2026, 2,  'M3'),

    (4, 'Z7777', 2025, 12, 'M4'),
    (4, 'Z7777', 2026, 1,  'M4'),
    (4, 'Z7777', 2026, 2,  'M4'),

    (6, 'Z9999', 2025, 12, 'M6'),
    (6, 'Z9999', 2026, 1,  'M6'),
    (6, 'Z9999', 2026, 2,  'M6'),

    (7, 'Z1234', 2025, 12, 'X7'),
    (7, 'Z1234', 2026, 1,  'X7'),
    (7, 'Z1234', 2026, 2,  'X7')
 ;

-- 'M5' omitted
-- Patients 1, 2, 3 have a new MBI for 2026-03
INSERT INTO v3.coverage_v3_staging(patient_id, contract, "year", "month", current_mbi)
VALUES
    (1, 'Z0000', 2025, 12, 'M1'),
    (1, 'Z0000', 2026, 1,  'M1'),
    (1, 'Z0000', 2026, 2,  'M1'),
    (1, 'Z0000', 2026, 3,  'X1'),

    (2, 'Z0000', 2025, 12, 'M2'),
    (2, 'Z0000', 2026, 1,  'M2'),
    (2, 'Z0000', 2026, 2,  'M2'),
    (2, 'Z0000', 2026, 3,  'X2'),

    (3, 'Z0000', 2025, 12, 'M3'),
    (3, 'Z0000', 2026, 1,  'M3'),
    (3, 'Z0000', 2026, 2,  'M3'),
    (3, 'Z0000', 2026, 3,  'X3'),

    (4, 'Z7777', 2025, 12, 'M4'),
    (4, 'Z7777', 2026, 1,  'M4'),
    (4, 'Z7777', 2026, 2,  'M4'),
    (4, 'Z7777', 2026, 3,  'M4'),

    (6, 'Z9999', 2025, 12, 'M6'),
    (6, 'Z9999', 2026, 1,  'M6'),
    (6, 'Z9999', 2026, 2,  'M6'),
    (6, 'Z9999', 2026, 3,  'M6'),

    (7, 'Z1234', 2025, 12, 'X7'),
    (7, 'Z1234', 2026, 1,  'X7'),
    (7, 'Z1234', 2026, 2,  'X7'),
    (7, 'Z1234', 2026, 3,  'X7')
 ;

-- MBI 'M5', 'X1', 'X2', 'X3' omitted intentionally
-- MBI 'M7' does not exist in coverage tables
INSERT INTO current_mbi(mbi, effective_date, share_data)
VALUES
    ('M1', '2025-01-01', false),
    ('M2', '2025-01-01', true),
    ('M3', '2025-01-01', false),
    ('M4', '2025-01-01', null),
    ('M6', '2025-01-01', false),
    ('M7', '2025-01-01', true)
;


CREATE TABLE IF NOT EXISTS job
(
    job_uuid character varying(255) NOT NULL,
    status character varying(32)  NOT NULL,
    fhir_version character varying(255)  NOT NULL,
    contract_number character varying(255) NOT NULL
);

INSERT INTO job(job_uuid, status, fhir_version, contract_number)
VALUES
    ('111A', 'SUBMITTED', 'R4V3', 'Z0001'),
    ('111B', 'SUBMITTED', 'R4V3', 'Z0002')
;

CREATE TABLE v3.coverage_v3_history_summary AS
SELECT
    contract,
    patient_id,
    current_mbi,
    array_agg(array[year, month] ORDER BY year ASC, month ASC) AS historical_coverage_summaries
FROM v3.coverage_v3_historical
WHERE false  -- no data yet
GROUP BY contract, patient_id, current_mbi;


DO $$
DECLARE
    c text;
    total int := 0;
BEGIN
    FOR c IN
        SELECT DISTINCT contract
        FROM v3.coverage_v3_historical
        ORDER BY contract
    LOOP
        INSERT INTO v3.coverage_v3_history_summary
        SELECT
            contract,
            patient_id,
            current_mbi,
            array_agg(array[year, month] ORDER BY year ASC, month ASC) AS historical_coverage_summaries
        FROM v3.coverage_v3_historical
        WHERE contract = c
        GROUP BY contract, patient_id, current_mbi
        ORDER BY contract, patient_id ASC, current_mbi;

        total := total + 1;
        RAISE NOTICE 'Processed contract % (% total)', c, total;

        COMMIT;  -- frees temp space after each contract to fit in 32GB RAM
    END LOOP;

    RAISE NOTICE 'Done. Processed % contracts total.', total;
END $$;


--DO $$
--DECLARE
--    c text;
--    total int := 0;
--BEGIN
--    FOR c IN
--        SELECT DISTINCT contract
--        FROM v3.coverage_v3
--        ORDER BY contract
--    LOOP
--
--        EXECUTE format('CREATE TABLE IF NOT EXISTS v3.coverage_v3_%s AS
--        (
--            select contract, patient_id, current_mbi, array_agg(array[year,month]) as recent_coverage_summaries
--                from (
--                    select * from v3.coverage_v3
--                    where contract=''%s''
--                    order by year asc, month asc
--                )
--                group by contract, patient_id, current_mbi
--                order by contract, patient_id asc, current_mbi
--        )', c, c);
--
--        total := total + 1;
--        RAISE NOTICE 'Processed contract % (% total)', c, total;
--
--        COMMIT;  -- frees temp space after each contract to fit in 32GB RAM
--    END LOOP;
--
--    RAISE NOTICE 'Done. Processed % contracts total.', total;
--END $$;

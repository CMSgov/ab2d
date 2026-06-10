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


CREATE SCHEMA contract;

CREATE TABLE IF NOT EXISTS contract.contract
(
    contract_number character varying(255) NOT NULL,
    attested_on timestamp with time zone,
    hpms_end_date timestamp with time zone
);

-- Used by isContractAttested() and deleteInactiveContractsFromHistorySummary()
-- ATT1: attested, no end date                       -> attested, not purged
-- NOATT: not attested                               -> not attested
-- OLD1/OLD2: attested, hpms_end_date > 2 years ago  -> purged from history summary
-- RECENT1: attested, hpms_end_date 1 year ago       -> retained
-- NULLEND: attested, hpms_end_date is null          -> retained
-- UNATT1: unattested, hpms_end_date is null         -> purged from history summary
INSERT INTO contract.contract(contract_number, attested_on, hpms_end_date)
VALUES
    ('ATT1',    now(), null),
    ('NOATT',   null,  null),
    ('OLD1',    now(), now() - interval '3 years'),
    ('OLD2',    now(), now() - interval '5 years'),
    ('RECENT1', now(), now() - interval '1 year'),
    ('NULLEND', now(), null),
    ('UNATT1',  null,  null)
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

-- Performs same function as populateHistorySummaryForContract()
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

-- History summary rows used by deleteInactiveContractsFromHistorySummary().
-- OLD1/OLD2 (attested, hpms_end_date > 2 years ago) and UNATT1 (unattested) must be purged;
-- RECENT1 (attested, 1 year ago) and NULLEND (attested, null end date) must remain.
INSERT INTO v3.coverage_v3_history_summary(contract, patient_id, current_mbi, historical_coverage_summaries)
VALUES
    ('OLD1',    101, 'P101', array[array[2020, 1]]),
    ('OLD1',    102, 'P102', array[array[2020, 2]]),
    ('OLD2',    103, 'P103', array[array[2019, 1]]),
    ('RECENT1', 104, 'P104', array[array[2025, 1]]),
    ('NULLEND', 105, 'P105', array[array[2025, 2]]),
    ('UNATT1',  106, 'P106', array[array[2024, 1]])
;

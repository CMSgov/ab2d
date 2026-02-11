CREATE SCHEMA v3;

CREATE TABLE IF NOT EXISTS v3.coverage_v3
(
    patient_id bigint NOT NULL,
    contract character varying(15)  NOT NULL,
    "year" integer NOT NULL,
    "month" integer NOT NULL,
    current_mbi character varying(32),
    historic_mbis character varying(256),
    CONSTRAINT coverage_v3_unique UNIQUE (patient_id, contract, "year", "month", current_mbi)
);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical
(
    patient_id bigint NOT NULL,
    contract character varying(15)  NOT NULL,
    "year" integer NOT NULL,
    "month" integer NOT NULL,
    current_mbi character varying(32),
    historic_mbis character varying(256),
    CONSTRAINT coverage_v3_historical_unique UNIQUE (patient_id, contract, "year", "month", current_mbi)
);

CREATE TABLE IF NOT EXISTS current_mbi
(
    mbi character varying(32) NOT NULL,
    effective_date date,
    share_data boolean
);

-- 'M3' and 'M4' omitted
INSERT INTO v3.coverage_v3_historical(patient_id, contract, "year", "month", current_mbi, historic_mbis)
VALUES
    (6, 'Z9999', 2025, 9,  'M6', 'M123,M456'),
    (6, 'Z9999', 2025, 10, 'M6', 'M123,M456'),
    (6, 'Z9999', 2025, 11, 'M6', 'M123,M456'),

    (1, 'Z0000', 2025, 6,  'M1', null),
    (1, 'Z0000', 2025, 7,  'M1', null),
    (1, 'Z0000', 2025, 8,  'M1', null),
    (1, 'Z0000', 2025, 9,  'M1', null),
    (1, 'Z0000', 2025, 10, 'M1', null),
    (1, 'Z0000', 2025, 11, 'M1', null),

    (2, 'Z0000', 2025, 9,  'M2', null),
    (2, 'Z0000', 2025, 10, 'M2', null),
    (2, 'Z0000', 2025, 11, 'M2', null),

    (5, 'Z8888', 2025, 7,  'M5', null),
    (5, 'Z8888', 2025, 8,  'M5', null),
    (5, 'Z8888', 2025, 9,  'M5', null)
;

-- 'M5' omitted
-- Only 'M6' has historic_mbis
INSERT INTO v3.coverage_v3(patient_id, contract, "year", "month", current_mbi, historic_mbis)
VALUES
    (1, 'Z0000', 2025, 12, 'M1', null),
    (1, 'Z0000', 2026, 1,  'M1', null),
    (1, 'Z0000', 2026, 2,  'M1', null),

    (2, 'Z0000', 2025, 12, 'M2', null),
    (2, 'Z0000', 2026, 1,  'M2', null),
    (2, 'Z0000', 2026, 2,  'M2', null),

    (3, 'Z0000', 2025, 12, 'M3', null),
    (3, 'Z0000', 2026, 1,  'M3', null),
    (3, 'Z0000', 2026, 2,  'M3', null),

    (4, 'Z7777', 2025, 12, 'M4', null),
    (4, 'Z7777', 2026, 1,  'M4', null),
    (4, 'Z7777', 2026, 2,  'M4', null),

    (6, 'Z9999', 2025, 12, 'M6', 'M123,M456'),
    (6, 'Z9999', 2026, 1,  'M6', 'M123,M456'),
    (6, 'Z9999', 2026, 2,  'M6', 'M123,M456')
 ;

-- MBI 'M5' omitted intentionally
INSERT INTO current_mbi(mbi, effective_date, share_data)
VALUES
    ('M1', '2025-01-01', false),
    ('M2', '2025-01-01', true),
    ('M3', '2025-01-01', false),
    ('M4', '2025-01-01', null),
    ('M6', '2025-01-01', false)
;

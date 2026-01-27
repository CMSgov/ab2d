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

INSERT INTO v3.coverage_v3_historical(patient_id, contract, "year", "month", current_mbi, historic_mbis)
VALUES
    (1, 'Z1234', 2025, 6,  'MBI_1', null),
    (1, 'Z1234', 2025, 7,  'MBI_1', null),
    (1, 'Z1234', 2025, 8,  'MBI_1', null),
    (1, 'Z1234', 2025, 9,  'MBI_1', null),
    (1, 'Z1234', 2025, 10, 'MBI_1', null),
    (1, 'Z1234', 2025, 11, 'MBI_1', null)
    ;

INSERT INTO v3.coverage_v3(patient_id, contract, "year", "month", current_mbi, historic_mbis)
VALUES
    (1, 'Z1234', 2025, 12, 'MBI_1', null),
    (1, 'Z1234', 2026, 1,  'MBI_1', null),
    (1, 'Z1234', 2026, 2,  'MBI_1', null)
    ;

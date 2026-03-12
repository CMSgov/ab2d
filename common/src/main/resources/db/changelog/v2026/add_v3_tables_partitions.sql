-- Create main tables for v3 and the partitions

CREATE TABLE IF NOT EXISTS v3.coverage_v3 (
    patient_id BIGINT NOT NULL,
    contract VARCHAR(15) NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    current_mbi VARCHAR(32),
    CONSTRAINT coverage_v3_unique
    UNIQUE (patient_id, contract, year, month, current_mbi)
    ) PARTITION BY LIST (contract);


CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical (
    patient_id BIGINT NOT NULL,
    contract VARCHAR(15) NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    current_mbi VARCHAR(32),
    CONSTRAINT coverage_v3_historical_unique
    UNIQUE (patient_id, contract, year, month, current_mbi)
    ) PARTITION BY LIST (contract);

-- Indexes

CREATE INDEX coverage_v3_patient_idx
    ON v3.coverage_v3 (patient_id, month);

CREATE INDEX coverage_v3_hist_patient_idx
    ON v3.coverage_v3_historical (patient_id, month);


-- CONTRACT PARTITIONS

CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('Z0000','Z0001','Z0002','Z0005','Z0010')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5960','S2893','S5743','S5540','S6506')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S4802','S5810','S5768')
    PARTITION BY LIST (year);

-- HISTORICAL CONTRACT PARTITIONS

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('Z0000','Z0001','Z0002','Z0005','Z0010')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5960','S2893','S5743','S5540','S6506')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S4802','S5810','S5768')
    PARTITION BY LIST (year);

-- SANDBOX YEAR PARTITIONS

CREATE TABLE v3.coverage_v3_sandbox_2020 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2020);
CREATE TABLE v3.coverage_v3_sandbox_2021 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2021);
CREATE TABLE v3.coverage_v3_sandbox_2022 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2022);
CREATE TABLE v3.coverage_v3_sandbox_2023 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2023);
CREATE TABLE v3.coverage_v3_sandbox_2024 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2024);
CREATE TABLE v3.coverage_v3_sandbox_2025 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2025);
CREATE TABLE v3.coverage_v3_sandbox_2026 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2026);
CREATE TABLE v3.coverage_v3_sandbox_2027 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2027);
CREATE TABLE v3.coverage_v3_sandbox_2028 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2028);
CREATE TABLE v3.coverage_v3_sandbox_2029 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2029);
CREATE TABLE v3.coverage_v3_sandbox_2030 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2030);

-- BCBS YEAR PARTITIONS

CREATE TABLE v3.coverage_v3_bcbs_2020 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2020);
CREATE TABLE v3.coverage_v3_bcbs_2021 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2021);
CREATE TABLE v3.coverage_v3_bcbs_2022 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2022);
CREATE TABLE v3.coverage_v3_bcbs_2023 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2023);
CREATE TABLE v3.coverage_v3_bcbs_2024 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2024);
CREATE TABLE v3.coverage_v3_bcbs_2025 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2025);
CREATE TABLE v3.coverage_v3_bcbs_2026 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2026);
CREATE TABLE v3.coverage_v3_bcbs_2027 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2027);
CREATE TABLE v3.coverage_v3_bcbs_2028 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2028);
CREATE TABLE v3.coverage_v3_bcbs_2029 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2029);
CREATE TABLE v3.coverage_v3_bcbs_2030 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2030);

-- CENTENE YEAR PARTITIONS

CREATE TABLE v3.coverage_v3_centene_2020 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2020);
CREATE TABLE v3.coverage_v3_centene_2021 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2021);
CREATE TABLE v3.coverage_v3_centene_2022 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2022);
CREATE TABLE v3.coverage_v3_centene_2023 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2023);
CREATE TABLE v3.coverage_v3_centene_2024 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2024);
CREATE TABLE v3.coverage_v3_centene_2025 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2025);
CREATE TABLE v3.coverage_v3_centene_2026 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2026);
CREATE TABLE v3.coverage_v3_centene_2027 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2027);
CREATE TABLE v3.coverage_v3_centene_2028 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2028);
CREATE TABLE v3.coverage_v3_centene_2029 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2029);
CREATE TABLE v3.coverage_v3_centene_2030 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2030);

-- DEFAULT PARTITIONS

CREATE TABLE v3.coverage_v3_default
    PARTITION OF v3.coverage_v3 DEFAULT;

CREATE TABLE v3.coverage_v3_historical_default
    PARTITION OF v3.coverage_v3_historical DEFAULT;
--V3 tables,indexes and partitions

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

CREATE TABLE IF NOT EXISTS v3.coverage_v3_staging (
                                                      patient_id BIGINT NOT NULL,
                                                      contract VARCHAR(15) NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    current_mbi VARCHAR(32),
    CONSTRAINT coverage_v3_staging_unique
    UNIQUE (patient_id, contract, year, month, current_mbi)
    );

-- INDEXES
CREATE INDEX IF NOT EXISTS coverage_v3_patient_idx ON v3.coverage_v3 (patient_id, month);
CREATE INDEX IF NOT EXISTS coverage_v3_hist_patient_idx ON v3.coverage_v3_historical (patient_id, month);
CREATE INDEX IF NOT EXISTS coverage_v3_stag_patient_idx ON v3.coverage_v3_staging (patient_id, month);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('Z0000','Z0001','Z0002','Z0005','Z0010','Z1001','Z1002','Z1003','Z1004','Z1005','Z1006','Z1007','Z1008','Z1009')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2020 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2021 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2022 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2023 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2024 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2025 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2026 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2027 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2028 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2029 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_sandbox_2030 PARTITION OF v3.coverage_v3_sandbox FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5960','S2893','S5743','S5540','S6506','S5726','S5584','S1030','S5953','S5450','S2468','S8067','S5715','S5593','S1140','S5993','S6875')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2020 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2021 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2022 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2023 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2024 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2025 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2026 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2027 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2028 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2029 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_bcbs_2030 PARTITION OF v3.coverage_v3_bcbs FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S4802','S5810','S5768')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2020 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2021 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2022 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2023 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2024 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2025 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2026 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2027 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2028 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2029 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_centene_2030 PARTITION OF v3.coverage_v3_centene FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S8182','S5596','S3375','S5805','S8841')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2020 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2021 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2022 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2023 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2024 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2025 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2026 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2027 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2028 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2029 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_anthem_united_2030 PARTITION OF v3.coverage_v3_anthem_united FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5617','S5983')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2020 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2021 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2022 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2023 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2024 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2025 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2026 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2027 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2028 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2029 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna1_2030 PARTITION OF v3.coverage_v3_cigna1 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5660')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2020 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2021 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2022 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2023 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2024 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2025 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2026 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2027 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2028 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2029 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cigna2_2030 PARTITION OF v3.coverage_v3_cigna2 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5601')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2020 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2021 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2022 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2023 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2024 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2025 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2026 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2027 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2028 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2029 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_cvs_2030 PARTITION OF v3.coverage_v3_cvs FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5884','S5552','S2874')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2020 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2021 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2022 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2023 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2024 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2025 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2026 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2027 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2028 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2029 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_humana_2030 PARTITION OF v3.coverage_v3_humana FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('E3014','S5877','S5966','S9325','S5904','S3994','S0655','S1822','E0654','S4501','S3521','S3875','S3285','E4744','S5975','S0586','S5588','S8677','S2465','S5857','S1894','S2668','S4219','S3389','S5795','S5753')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2020 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2021 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2022 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2023 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2024 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2025 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2026 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2027 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2028 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2029 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_misc_2030 PARTITION OF v3.coverage_v3_misc FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S6946','S5609','S5916','S7126','S9701','S7694')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2020 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2021 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2022 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2023 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2024 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2025 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2026 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2027 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2028 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2029 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_mutual_dean_clear_cambia_rite_2030 PARTITION OF v3.coverage_v3_mutual_dean_clear_cambia_rite FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5921')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2020 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2021 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2022 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2023 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2024 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2025 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2026 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2027 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2028 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2029 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united1_2030 PARTITION OF v3.coverage_v3_united1 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_united2
    PARTITION OF v3.coverage_v3
    FOR VALUES IN ('S5820')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_united_2020 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united_2021 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united_2022 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united_2023 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united_2024 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united_2025 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united2_2026 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united2_2027 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united2_2028 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united2_2029 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_united2_2030 PARTITION OF v3.coverage_v3_united2 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_default
    PARTITION OF v3.coverage_v3 DEFAULT;

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('Z0000','Z0001','Z0002','Z0005','Z0010','Z1001','Z1002','Z1003','Z1004','Z1005','Z1006','Z1007','Z1008','Z1009')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2020 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2021 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2022 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2023 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2024 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2025 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2026 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2027 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2028 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2029 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_sandbox_2030 PARTITION OF v3.coverage_v3_historical_sandbox FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5960','S2893','S5743','S5540','S6506','S5726','S5584','S1030','S5953','S5450','S2468','S8067','S5715','S5593','S1140','S5993','S6875')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2020 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2021 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2022 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2023 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2024 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2025 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2026 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2027 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2028 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2029 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_bcbs_2030 PARTITION OF v3.coverage_v3_historical_bcbs FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S4802','S5810','S5768')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2020 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2021 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2022 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2023 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2024 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2025 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2026 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2027 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2028 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2029 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_centene_2030 PARTITION OF v3.coverage_v3_historical_centene FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S8182','S5596','S3375','S5805','S8841')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2020 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2021 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2022 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2023 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2024 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2025 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2026 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2027 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2028 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2029 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_anthem_united_2030 PARTITION OF v3.coverage_v3_historical_anthem_united FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5617','S5983')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2020 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2021 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2022 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2023 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2024 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2025 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2026 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2027 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2028 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2029 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna1_2030 PARTITION OF v3.coverage_v3_historical_cigna1 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5660')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2020 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2021 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2022 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2023 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2024 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2025 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2026 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2027 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2028 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2029 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cigna2_2030 PARTITION OF v3.coverage_v3_historical_cigna2 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5601')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2020 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2021 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2022 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2023 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2024 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2025 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2026 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2027 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2028 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2029 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_cvs_2030 PARTITION OF v3.coverage_v3_historical_cvs FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5884','S5552','S2874')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2020 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2021 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2022 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2023 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2024 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2025 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2026 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2027 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2028 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2029 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_humana_2030 PARTITION OF v3.coverage_v3_historical_humana FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('E3014','S5877','S5966','S9325','S5904','S3994','S0655','S1822','E0654','S4501','S3521','S3875','S3285','E4744','S5975','S0586','S5588','S8677','S2465','S5857','S1894','S2668','S4219','S3389','S5795','S5753')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2020 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2021 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2022 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2023 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2024 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2025 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2026 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2027 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2028 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2029 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_misc_2030 PARTITION OF v3.coverage_v3_historical_misc FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S6946','S5609','S5916','S7126','S9701','S7694')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2020 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2021 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2022 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2023 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2024 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2025 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2026 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2027 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2028 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2029 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_mutual_dean_clear_cambia_rite_2030 PARTITION OF v3.coverage_v3_historical_mutual_dean_clear_cambia_rite FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5921')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2020 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2021 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2022 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2023 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2024 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2025 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2026 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2027 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2028 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2029 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united1_2030 PARTITION OF v3.coverage_v3_historical_united1 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united2
    PARTITION OF v3.coverage_v3_historical
    FOR VALUES IN ('S5820')
    PARTITION BY LIST (year);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united_2020 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2020);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united_2021 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2021);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united_2022 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2022);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united_2023 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2023);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united_2024 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2024);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united_2025 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2025);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united2_2026 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2026);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united2_2027 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2027);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united2_2028 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2028);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united2_2029 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2029);
CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_united2_2030 PARTITION OF v3.coverage_v3_historical_united2 FOR VALUES IN (2030);

CREATE TABLE IF NOT EXISTS v3.coverage_v3_historical_default
    PARTITION OF v3.coverage_v3_historical DEFAULT;
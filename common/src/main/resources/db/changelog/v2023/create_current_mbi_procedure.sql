CREATE OR REPLACE PROCEDURE update_current_mbi_2023()
LANGUAGE plpgsql
AS $$
begin
INSERT INTO current_mbi
SELECT DISTINCT current_mbi from coverage_anthem_united_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_bcbs_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_centene_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_cigna1_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_cigna2_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_cvs_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_centene_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_humana_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_misc_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_mutual_dean_clear_cambia_rite_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_united1_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_united_2023
UNION DISTINCT
SELECT DISTINCT current_mbi from coverage_default
UNION DISTINCT
SELECT DISTINCT current_mbi from sandbox_2023
ON CONFLICT DO NOTHING;
end;
$$;
--
-- CREATE OR REPLACE PROCEDURE update_current_mbi_2024()
--     LANGUAGE plpgsql
-- AS $$
-- begin
--     INSERT INTO current_mbi
--     SELECT DISTINCT current_mbi from coverage_anthem_united_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_bcbs_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_centene_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_cigna1_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_cigna2_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_cvs_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_centene_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_humana_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_misc_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_mutual_dean_clear_cambia_rite_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_united1_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_united_2024
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from coverage_default
--     UNION DISTINCT
--     SELECT DISTINCT current_mbi from sandbox_2024
--     ON CONFLICT DO NOTHING;
-- end;
-- $$;
--
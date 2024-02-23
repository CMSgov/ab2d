CREATE OR REPLACE PROCEDURE update_current_mbi_2023()
LANGUAGE plpgsql
AS $$
begin
INSERT INTO current_mbi
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_anthem_united_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_bcbs_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_centene_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_cigna1_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_cigna2_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_cvs_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_centene_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_humana_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_misc_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_mutual_dean_clear_cambia_rite_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_united1_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_united_2023 WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_default WHERE current_mbi IS NOT NULL
UNION DISTINCT
SELECT DISTINCT current_mbi, effective_date, opt_out_flag from sandbox_2023 WHERE current_mbi IS NOT NULL
ON CONFLICT DO NOTHING;
end;
$$;

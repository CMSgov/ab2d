CREATE OR REPLACE PROCEDURE update_current_mbi_2024()
    LANGUAGE plpgsql
AS $$
begin
    INSERT INTO current_mbi
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_anthem_united_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_bcbs_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_centene_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_cigna1_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_cigna2_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_cvs_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_centene_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_humana_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_misc_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_mutual_dean_clear_cambia_rite_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_united1_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_united_2024 WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from coverage_default WHERE current_mbi IS NOT NULL
    UNION DISTINCT
    SELECT DISTINCT current_mbi, effective_date, opt_out_flag from sandbox_2024 WHERE current_mbi IS NOT NULL
    ON CONFLICT DO NOTHING;
end;
$$;

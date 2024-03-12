--AB2D-6007
--create view with mbi as a column ,simple views update automatically ,no refresh needed
CREATE OR REPLACE VIEW public.coverage_view_with_mbi
 AS

SELECT cov.contract,
       cov.year,
       cov.month,
       cov.beneficiary_id,
       cov.current_mbi,
       cov.bene_coverage_period_id,
       cov.bene_coverage_search_event_id
FROM coverage cov;

ALTER TABLE public.coverage_view
    OWNER TO cmsadmin;

GRANT SELECT ON TABLE public.coverage_view TO ab2d_analyst;
GRANT ALL ON TABLE public.coverage_view TO cmsadmin;


ALTER TABLE current_mbi ADD COLUMN IF NOT EXISTS opt_out_flag BOOLEAN ; --add new columns
ALTER TABLE current_mbi ADD COLUMN IF NOT EXISTS effective_date TIMESTAMP;

ALTER TABLE coverage ALTER COLUMN opt_out_flag DROP DEFAULT ; --drop default constraint
--update  current_mbi set opt_out_flag=NULL  RUN ONLY ONCE if table is created with default as "fault"

--/*******RUN ONLY ONCE WHEN TABLE IS EMPTY to pul historic data *****/
-- SELECT distinct(current_mbi)
-- FROM coverage_view_with_mbi
-- WHERE contract  in (
-- SELECT DISTINCT(contract_number)
--                        FROM job_view
--                        WHERE contract_number not LIKE 'Z%' --OR contract_number LIKE 'E%'
--                   )

-- 					   AND year IN (2020, 2021, 2022, 2023);

Drop procedure update_current_mbi_2023();

--update update_current_mbi_2024() to remove sandbox contracts to be pushed to current_mbi table
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
    ON CONFLICT DO NOTHING;
end;
$$;

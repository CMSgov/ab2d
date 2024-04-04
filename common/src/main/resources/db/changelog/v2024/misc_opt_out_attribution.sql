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
FROM public.coverage cov;

ALTER TABLE public.current_mbi ADD COLUMN IF NOT EXISTS opt_out_flag BOOLEAN ; --add new columns
ALTER TABLE public.current_mbi ADD COLUMN IF NOT EXISTS effective_date DATE;
ALTER TABLE public.current_mbi ALTER COLUMN opt_out_flag DROP DEFAULT ;
ALTER TABLE public.coverage DROP COLUMN opt_out_flag  ;
ALTER TABLE public.coverage DROP COLUMN effective_date  ;


--update  current_mbi set opt_out_flag=NULL  RUN ONLY ONCE if table is created with default as "fault"

--/*******RUN ONLY ONCE WHEN TABLE IS EMPTY without "effective_date> CURRENT_DATE - 1" to pull historic data *****/
--TODO Set pg_cron schedule to get new PDP enrollments ****/
Create or replace procedure public.insert_new_current_mbi()
LANGUAGE plpgsql
AS $$
begin
insert into public.current_mbi(
    mbi)
SELECT distinct current_mbi
FROM coverage_view_with_mbi
WHERE contract  in (
    SELECT DISTINCT(contract_number)
    FROM job_view
    WHERE contract_number not LIKE 'Z%'
)
on conflict do nothing;
end;
$$;
-- /******* END ******/




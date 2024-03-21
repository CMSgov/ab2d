--AB2D-6007
--create view with mbi as a column ,simple views update automatically ,no refresh needed
CREATE OR REPLACE VIEW public.coverage_view_with_mbi
 AS
SELECT cov.contract,
       cov.year,
       cov.month,
       cov.beneficiary_id,
       cov.current_mbi,
       cov.effective_date,
       cov.opt_out_flag,
       cov.bene_coverage_period_id,
       cov.bene_coverage_search_event_id
FROM public.coverage cov;

ALTER TABLE public.current_mbi ADD COLUMN IF NOT EXISTS opt_out_flag BOOLEAN ; --add new columns
ALTER TABLE public.current_mbi ADD COLUMN IF NOT EXISTS effective_date TIMESTAMP;

ALTER TABLE public.coverage ALTER COLUMN opt_out_flag DROP DEFAULT ; --drop default constraint
ALTER TABLE public.current_mbi ALTER COLUMN opt_out_flag DROP DEFAULT ;
--update  current_mbi set opt_out_flag=NULL  RUN ONLY ONCE if table is created with default as "fault"

--/*******RUN ONLY ONCE WHEN TABLE IS EMPTY without "effective_date> CURRENT_DATE - 1" to pull historic data *****/
--TODO Set pg_cron schedule to get new PDP enrollments ****/
Create or replace procedure public.insert_new_current_mbi()
LANGUAGE plpgsql
AS $$
begin
insert into public.current_mbi(
    mbi, effective_date, opt_out_flag)
SELECT distinct current_mbi,effective_date,opt_out_flag
FROM coverage_view_with_mbi
WHERE contract  in (
    SELECT DISTINCT(contract_number)
    FROM job_view
    WHERE contract_number not LIKE 'Z%'
) and effective_date> CURRENT_DATE - 1
on conflict do nothing
-- /******* END ******/

--- /*****RUNS on schedule weekly --TODO Set pg_cron schedule and update proc name ****/
Create or replace procedure public.update_current_mbi()
LANGUAGE plpgsql
AS $$
begin
update public.current_mbi  set mbi =sub.current_mbi,effective_date=sub.effective_date,opt_out_flag=sub.opt_out_flag
    from
(SELECT distinct current_mbi,effective_date,opt_out_flag
FROM coverage_view_with_mbi
WHERE contract  in (
SELECT DISTINCT(contract_number)
                       FROM job_view
                      WHERE contract_number not LIKE 'Z%' --OR contract_number LIKE 'E%'
                  ))  sub where current_mbi.mbi=sub.current_mbi;
end;
$$;
-- /******* END ******/



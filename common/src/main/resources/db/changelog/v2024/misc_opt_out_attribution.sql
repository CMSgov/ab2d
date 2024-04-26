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
ALTER TABLE public.current_mbi ADD COLUMN IF NOT EXISTS effective_date TIMESTAMP;
ALTER TABLE public.current_mbi ALTER COLUMN opt_out_flag DROP DEFAULT ;
ALTER TABLE public.coverage DROP COLUMN IF EXISTS opt_out_flag  ;
ALTER TABLE public.coverage DROP COLUMN IF EXISTS effective_date  ;

ALTER TABLE current_mbi ALTER COLUMN effective_date TYPE DATE;

-- view below due to time out issues and to address AB2D-6051

CREATE OR REPLACE Procedure proc_insert_mbi_to_table(
var_offset int,
var_limit int default 0)
AS
$$
DECLARE
v_cnt int;
    contract_cursor CURSOR FOR
Select distinct contract_number from contract_view where contract_number not like '%Z%' and contract_number not like '%E%'order by contract_number OFFSET(var_offset) limit(var_limit) ;
contract_record RECORD;
BEGIN
-- Open cursor
SET statement_timeout = 0;
OPEN contract_cursor;
RAISE NOTICE 'Start';
--Fetch rows and return
LOOP
FETCH NEXT FROM contract_cursor INTO contract_record;
        EXIT WHEN NOT FOUND;

-- INSERT Contract and get count from INSERT
INSERT into public.current_mbi(mbi) SELECT distinct current_mbi
FROM coverage_view_with_mbi WHERE contract  in (contract_record.contract_number)
                              and current_mbi is not null  order by current_mbi on conflict do nothing;

get diagnostics v_cnt = row_count; -- get count
RAISE NOTICE 'Contract: % Value: %' , contract_record.contract_number, v_cnt;
END LOOP;
RAISE NOTICE 'All Done!';
-- Close cursor

CLOSE contract_cursor;
COMMIT;
END;
$$
LANGUAGE PLPGSQL;






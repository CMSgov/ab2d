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

-- DROP PROCEDURE ab2d.proc_insert_mbi_to_table(int, int);

CREATE OR REPLACE PROCEDURE ab2d.proc_insert_mbi_to_table(
    IN var_offset integer,
    IN var_limit integer DEFAULT 0
)
LANGUAGE plpgsql
AS $procedure$
DECLARE
v_cnt int;
    contract_record RECORD;

    contract_cursor CURSOR FOR
SELECT contract_number
FROM contract_view
WHERE
    (
                EXTRACT(DAY FROM CURRENT_DATE) NOT IN (1, 15)  -- Any day except 1st or 15th
            AND contract_type = 'NORMAL'
            AND attested_on IS NOT NULL
            AND enabled = TRUE
        )
   OR
    (
                EXTRACT(DAY FROM CURRENT_DATE) IN (1, 15)  -- Only on 1st or 15th
            AND contract_number LIKE 'Z%'
            OR (contract_type = 'NORMAL'
            AND attested_on IS NOT NULL
            AND enabled = TRUE)
        )
ORDER BY contract_number
OFFSET var_offset
    LIMIT var_limit;
BEGIN
    -- Disable statement timeout
    SET statement_timeout = 0;

OPEN contract_cursor;
RAISE NOTICE 'Start';

    LOOP
FETCH contract_cursor INTO contract_record;
        EXIT WHEN NOT FOUND;

        -- Insert distinct current_mbi for contract
INSERT INTO current_mbi(mbi)
SELECT DISTINCT current_mbi
FROM coverage_view_with_mbi
WHERE contract = contract_record.contract_number
  AND current_mbi IS NOT NULL
    ON CONFLICT DO NOTHING;

GET DIAGNOSTICS v_cnt = ROW_COUNT; -- get count of inserted rows
RAISE NOTICE 'Contract: % Value: %', contract_record.contract_number, v_cnt;
END LOOP;

    RAISE NOTICE 'All Done!';

CLOSE contract_cursor;

COMMIT;
END;
$procedure$;






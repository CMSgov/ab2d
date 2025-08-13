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
            AND enabled = TRUE
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
-- Partitions for 2026 to 2030 are generated and this function exists in prod already.
CREATE OR REPLACE FUNCTION ab2d.create_year_partition(contract_partition text, year_val integer)
 RETURNS void
 LANGUAGE plpgsql
AS $function$
DECLARE
partition_name text;
BEGIN
    partition_name := contract_partition || '_' || year_val;
    -- Create partition if not exists
EXECUTE format(
        'CREATE TABLE IF NOT EXISTS ab2d.%I PARTITION OF ab2d.%I FOR VALUES IN (%L);',
        partition_name, contract_partition, year_val
    );
-- Create indexes
EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I_bene_coverage_period_id_idx ON ab2d.%I (bene_coverage_period_id, beneficiary_id, contract, year);',
        partition_name, partition_name
    );
EXECUTE format(
        'CREATE INDEX IF NOT EXISTS %I_bene_coverage_search_event_id_idx ON ab2d.%I (bene_coverage_search_event_id, beneficiary_id, contract, year);',
        partition_name, partition_name
    );

END;
$function$
;

/*Usage to create new partitions :
     Change the year and change the PDP name. This checks for existing partitions n creates only if its missing */

DO $$
DECLARE
c text;
    y int;
BEGIN
FOR y IN 2026..2030 LOOP
        FOREACH c IN ARRAY ARRAY[
            'coverage_anthem_united',
            'coverage_bcbs',
            'coverage_centene'
        ] LOOP
            PERFORM ab2d.create_year_partition(c, y);
END LOOP;
END LOOP;
END;
$$;


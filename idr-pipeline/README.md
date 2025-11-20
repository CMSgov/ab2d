# AB2D IDR Pipeline

The IDR pipeline will be used in v3 of the AB2D API. 
For more information, see [IDR <> AB2D Attribution](https://confluence.cms.gov/spaces/ODI/pages/1372707089/IDR+AB2D+Attribution)

This project will be responsible for exporting IDR data from Snowflake and importing into the AB2D PostgreSQL database.

## Example Queries

**Note** about PHI/PII: 
- Snowflake houses production IDR data containing PHI/PII, notably `Medicare Beneficiary Identifier` (MBI) numbers.


1. All PDPs, All Months back to 2021 - Returns real MBI numbers

```sql
WITH month_series AS ( 
    SELECT DATE '2021-01-01' AS month_start
    UNION ALL
    SELECT ADD_MONTHS(month_start, 1)
    FROM month_series
    WHERE month_start < DATE '2025-12-01'
)
SELECT 
    bene.bene_xref_efctv_sk,
    elec.bene_cntrct_num,
    bene.bene_mbi_id, 
    substr(ms.month_start,0,7) AS coverage_month
FROM month_series ms
JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_MAPD_ENRLMT elec
    ON elec.bene_enrlmt_bgn_dt <= LAST_DAY(ms.month_start)
   AND elec.bene_enrlmt_end_dt >= ms.month_start
JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY bene
    ON elec.bene_sk = bene.bene_sk
WHERE elec.bene_cntrct_num LIKE 'S%' 
  AND elec.idr_ltst_trans_flg = 'Y'
  AND elec.idr_trans_obslt_ts > '9999-12-30'
  AND bene.idr_ltst_trans_flg = 'Y'
  AND bene.bene_xref_efctv_sk != 0
ORDER BY bene.bene_xref_efctv_sk, ms.month_start;
LIMIT 500;
```

Example output (using fake data):

|BENE_XREF_EFCTV_SK|BENE_CNTRCT_NUM|BENE_MBI_ID|COVERAGE_MONTH|
|------------------|---------------|-----------|--------------|
|1000              |S1234          |12345678999|2021-01       |
|1000              |S1234          |12345678999|2021-02       |
|1000              |S1234          |12345678999|2021-03       |
|1000              |S1234          |12345678999|2021-04       |
|1000              |S1234          |12345678999|2021-05       |
|1000              |S1234          |12345678999|2021-06       |
|1000              |S1234          |12345678999|2021-07       |
|1000              |S1234          |12345678999|2021-08       |
|1000              |S1234          |12345678999|2021-09       |
|1000              |S1234          |12345678999|2021-10       |
|1000              |S1234          |12345678999|2021-11       |
|1000              |S1234          |12345678999|2021-12       |
|1000              |S1234          |12345678999|2022-01       |
|1000              |S1234          |12345678999|2022-02       |



2. All PDPs, All Months back to 2021 - Returns semi-randomized MBI numbers and separate year/month columns; similar to current schema for the `coverage` table.

```sql
SET random_string=(select randstr(6, random()));

WITH month_series AS ( /
    SELECT DATE '2021-01-01' AS month_start
    UNION ALL
    SELECT ADD_MONTHS(month_start, 1)
    FROM month_series
    WHERE month_start < DATE '2025-12-01'
)
SELECT 
    bene.bene_xref_efctv_sk as patient_id,
    elec.bene_cntrct_num as contract,
    md5(concat($random_string, substr(bene.bene_mbi_id,0,6))) as mbi_randomized, 
    substr(ms.month_start,0,4) AS year,
    cast(substr(ms.month_start,6,2) AS INTEGER) AS month,
    
FROM month_series ms
JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_MAPD_ENRLMT elec
    ON elec.bene_enrlmt_bgn_dt <= LAST_DAY(ms.month_start)
   AND elec.bene_enrlmt_end_dt >= ms.month_start
JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY bene
    ON elec.bene_sk = bene.bene_sk
WHERE elec.bene_cntrct_num LIKE 'S%' 
  AND elec.idr_ltst_trans_flg = 'Y'
  AND elec.idr_trans_obslt_ts > '9999-12-30'
  AND bene.idr_ltst_trans_flg = 'Y'
  AND bene.bene_xref_efctv_sk != 0
ORDER BY bene.bene_xref_efctv_sk, ms.month_start
LIMIT 500;
```

Example output (using fake data):

|PATIENT_ID|CONTRACT                     |MBI_RANDOMIZED|YEAR                                         |MONTH|
|----------|-----------------------------|--------------|---------------------------------------------|-----|
|1000      |S1234                        |12345678999|2021                                         |1    |
|1000      |S1234                        |12345678999|2021                                         |2    |
|1000      |S1234                        |12345678999|2021                                         |3    |
|1000      |S1234                        |12345678999|2021                                         |4    |
|1000      |S1234                        |12345678999|2021                                         |5    |
|1000      |S1234                        |12345678999|2021                                         |6    |
|1000      |S1234                        |12345678999|2021                                         |7    |
|1000      |S1234                        |12345678999|2021                                         |8    |
|1000      |S1234                        |12345678999|2021                                         |9    |
|1000      |S1234                        |12345678999|2021                                         |10   |
|1000      |S1234                        |12345678999|2021                                         |11   |
|1000      |S1234                        |12345678999|2021                                         |12   |
|1000      |S1234                        |12345678999|2022                                         |1    |
|1000      |S1234                        |12345678999|2022                                         |2    |



3. All PDPs, All Months back to 2021 - Returns semi-randomized MBIs with aggregated coverage dates

```sql
SET random_string=(select randstr(6, random()));

SELECT DISTINCT patient_id, contract, mbi_randomized, LISTAGG(coverage_month, ', ') 
WITHIN GROUP (ORDER BY coverage_month)
OVER (PARTITION BY patient_id, contract, mbi_randomized) AS aggregated_coverage_dates
FROM (
    WITH month_series AS ( 
        SELECT DATE '2021-01-01' AS month_start
        UNION ALL
        SELECT ADD_MONTHS(month_start, 1)
        FROM month_series
        WHERE month_start < DATE '2025-12-01'
    )
    SELECT 
        bene.bene_xref_efctv_sk as patient_id,
        elec.bene_cntrct_num as contract,
        md5(concat($random_string, substr(bene.bene_mbi_id,0,6))) as mbi_randomized, 
        substr(ms.month_start,0,7) AS coverage_month
    
    FROM month_series ms
    JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_MAPD_ENRLMT elec
        ON elec.bene_enrlmt_bgn_dt <= LAST_DAY(ms.month_start)
       AND elec.bene_enrlmt_end_dt >= ms.month_start
    JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY bene
        ON elec.bene_sk = bene.bene_sk
    WHERE elec.bene_cntrct_num LIKE 'S%' 
      AND elec.idr_ltst_trans_flg = 'Y'
      AND elec.idr_trans_obslt_ts > '9999-12-30'
      AND bene.idr_ltst_trans_flg = 'Y'
      AND bene.bene_xref_efctv_sk != 0
    ORDER BY bene.bene_xref_efctv_sk, ms.month_start
    
    LIMIT 500
);
```

Example output (using fake data):

|PATIENT_ID|CONTRACT|MBI_RANDOMIZED|AGGREGATED_COVERAGE_DATES|
|----------|--------|--------------|-------------------------|
|1000      |S1234   |12345678999   |2021-01, 2021-02, 2021-03, 2021-04, 2021-05|
|5250      |S2345   |12345678900   |2021-01, 2021-02, 2021-03, 2021-04, 2021-05, 2021-06, 2021-07, 2021-08, 2021-09, 2021-10, 2021-11, 2021-12, 2022-01, 2022-02, 2022-03, 2022-04, 2022-05, 2022-06, 2022-07, 2022-08, 2022-09, 2022-10, 2022-11, 2022-12|
|1953      |S3456   |12345678901   |2025-06, 2025-07, 2025-08|
|6683      |S4567   |12345678902   |2021-01, 2021-02, 2021-03, 2021-04, 2021-05, 2021-06, 2021-07|

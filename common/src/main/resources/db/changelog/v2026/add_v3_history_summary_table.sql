CREATE TABLE IF NOT EXISTS v3.coverage_v3_history_summary AS
SELECT
    contract,
    patient_id,
    current_mbi,
    array_agg(array[year, month] ORDER BY year ASC, month ASC) AS historical_coverage_summaries
FROM v3.coverage_v3_historical
WHERE false
GROUP BY contract, patient_id, current_mbi;

CREATE INDEX ON v3.coverage_v3_history_summary (contract);
CREATE INDEX ON v3.coverage_v3_history_summary (patient_id);
CREATE INDEX ON v3.coverage_v3_history_summary (current_mbi);

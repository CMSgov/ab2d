CREATE TABLE IF NOT EXISTS v3.coverage_v3_history_summary_coverage_periods (
    contract TEXT NOT NULL,
    year INT NOT NULL,
    month INT NOT NULL,
    UNIQUE (contract, year, month)
);

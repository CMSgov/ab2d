CREATE TABLE IF NOT EXISTS v3.coverage_v3_audit (
    id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    action TEXT NOT NULL,
    result TEXT,
    contract TEXT,
    log TEXT NOT NULL,
    data JSONB
);

INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'v3.audit-logging.enabled', 'true', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'v3.audit-logging.enabled'); -- gitleaks:allow

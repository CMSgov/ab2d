INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'bfd.metrics.enabled', 'false', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'bfd.metrics.enabled'); -- gitleaks:allow

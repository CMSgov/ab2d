INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'eob.v3.inplace.enabled', 'false', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'eob.v3.inplace.enabled'); -- gitleaks:allow


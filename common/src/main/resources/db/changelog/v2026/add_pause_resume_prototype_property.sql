INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'pause-resume.prototype.enabled', 'false', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.prototype.enabled'); -- gitleaks:allow

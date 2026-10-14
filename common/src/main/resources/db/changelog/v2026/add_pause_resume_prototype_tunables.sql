-- adds the three prototype properties most likely to need changes without a redeploy
INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'pause-resume.prototype.partition-size', '1000', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.prototype.partition-size'); -- gitleaks:allow

INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'pause-resume.prototype.chunk-size', '100', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.prototype.chunk-size'); -- gitleaks:allow

INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'pause-resume.prototype.copy-forward-enabled', 'true', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.prototype.copy-forward-enabled'); -- gitleaks:allow

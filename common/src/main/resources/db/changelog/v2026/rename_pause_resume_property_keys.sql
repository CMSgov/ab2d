-- moves the pause/resume properties to the pause-resume.* key namespace, keeping any values already set
UPDATE property.properties SET key = 'pause-resume.enabled', modified = now()
    WHERE key = 'pause-resume.prototype.enabled'
    AND NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.enabled'); -- gitleaks:allow

UPDATE property.properties SET key = 'pause-resume.partition-size', modified = now()
    WHERE key = 'pause-resume.prototype.partition-size'
    AND NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.partition-size'); -- gitleaks:allow

UPDATE property.properties SET key = 'pause-resume.chunk-size', modified = now()
    WHERE key = 'pause-resume.prototype.chunk-size'
    AND NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.chunk-size'); -- gitleaks:allow

UPDATE property.properties SET key = 'pause-resume.copy-forward-enabled', modified = now()
    WHERE key = 'pause-resume.prototype.copy-forward-enabled'
    AND NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'pause-resume.copy-forward-enabled'); -- gitleaks:allow

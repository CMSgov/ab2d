INSERT INTO property.properties (id, key, value, created, modified)
SELECT (SELECT COALESCE(MAX(id),0)+1 FROM property.properties),
       'v3.whitelisted.contracts', '', now(), now()
    WHERE NOT EXISTS (SELECT 1 FROM property.properties WHERE key = 'v3.whitelisted.contracts');

UPDATE property.properties
SET value='Z0001,Z0002,Z0003,Z0004,Z0005,Z0006,Z0007,Z0008,Z0009,Z0010'
WHERE key='v3.whitelisted.contracts';

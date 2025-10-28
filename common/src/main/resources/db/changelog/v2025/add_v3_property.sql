INSERT INTO property.properties (id, key, value, created, modified)
SELECT nextval('hibernate_sequence'), 'v3.on', 'false', current_timestamp, current_timestamp
    WHERE NOT EXISTS (
    SELECT 1
    FROM property.properties
    WHERE key = 'v3.on'
);
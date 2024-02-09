INSERT INTO property.properties(
	id, key, value, created, modified)
SELECT nextval('hibernate_sequence'), 'OptOutOn', 'false', current_timestamp, current_timestamp
WHERE
NOT EXISTS (
SELECT * FROM property.properties 
                   WHERE key='OptOutOn'
);

--rollback DELETE FROM properties WHERE key = 'OptOutOn';

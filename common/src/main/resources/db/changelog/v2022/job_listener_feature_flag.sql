INSERT INTO properties (id, key, value)
VALUES((select nextval('hibernate_sequence')), 'sns_job_update.engaged', 'idle');

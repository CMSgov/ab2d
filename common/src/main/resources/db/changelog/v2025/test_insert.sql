-- Test insert with explicit schema
INSERT INTO ab2d.role (id, name) VALUES ((select nextval('hibernate_sequence')), 'TEST1');

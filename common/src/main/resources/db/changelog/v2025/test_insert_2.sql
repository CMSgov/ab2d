-- Test insert without explicit schema
INSERT INTO role (id, name) VALUES ((select nextval('hibernate_sequence')), 'TEST2');

--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset dkrylovsb:initial_data_seed failOnError:true
INSERT INTO role (id, name) VALUES ((select nextval('hibernate_sequence')), 'ADMIN');
INSERT INTO role (id, name) VALUES ((select nextval('hibernate_sequence')), 'SPONSOR');

INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 1, 'Haag-Goodwin', null, null);
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 999, 'Kutch LLC', null,
                                                                           (select id from sponsor where hpms_id=1));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'EileenCFrierson@example.com', 'Eileen', 'Frierson', 'EileenCFrierson@example.com', (select id from sponsor where hpms_id=999), true);

INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='EileenCFrierson@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0001', 'S0001',
                                                                                           (select id from sponsor where hpms_id=999), '2019-11-01');


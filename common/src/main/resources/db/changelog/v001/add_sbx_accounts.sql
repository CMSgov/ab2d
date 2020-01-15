INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 2, 'PDP-100', null, null);
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 3, 'PDP-1000', null, null);
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 4, 'PDP-2000', null, null);
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 5, 'PDP-2000', null, null);
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 6, 'PDP-5000', null, null);
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 7, 'PDP-10000', null, null);
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 8, 'PDP-30000', null, null);


INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'newuser1@example.com', 'New', 'User', 'newuser1@example.com', (select id from sponsor where hpms_id=2), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='newuser1@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'newuser2@example.com', 'New', 'User', 'newuser2@example.com', (select id from sponsor where hpms_id=3), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='newuser2@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'newuser3@example.com', 'New', 'User', 'newuser3@example.com', (select id from sponsor where hpms_id=4), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='newuser3@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'newuser4@example.com', 'New', 'User', 'newuser4@example.com', (select id from sponsor where hpms_id=5), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='newuser4@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'newuser5@example.com', 'New', 'User', 'newuser5@example.com', (select id from sponsor where hpms_id=6), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='newuser5@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'newuser6@example.com', 'New', 'User', 'newuser6@example.com', (select id from sponsor where hpms_id=7), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='newuser6@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'newuser7@example.com', 'New', 'User', 'newuser7@example.com', (select id from sponsor where hpms_id=8), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='newuser7@example.com'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0000', 'S0000',
                                                                                           (select id from sponsor where hpms_id=2), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0002', 'S0002',
                                                                                           (select id from sponsor where hpms_id=3), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0003', 'S0003',
                                                                                           (select id from sponsor where hpms_id=4), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0004', 'S0004',
                                                                                           (select id from sponsor where hpms_id=5), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0005', 'S0005',
                                                                                           (select id from sponsor where hpms_id=6), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0010', 'S0010',
                                                                                           (select id from sponsor where hpms_id=7), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0030', 'S0030',
                                                                                           (select id from sponsor where hpms_id=8), '2019-11-01');
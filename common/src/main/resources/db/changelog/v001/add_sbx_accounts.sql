INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 8, 'PDP-SBX', null, null);

INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 2, 'PDP-100', null, (select id from sponsor where hpms_id=8));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 3, 'PDP-1000', null, (select id from sponsor where hpms_id=8));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 4, 'PDP-2000', null, (select id from sponsor where hpms_id=8));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 5, 'PDP-5000', null, (select id from sponsor where hpms_id=8));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 6, 'PDP-10000', null, (select id from sponsor where hpms_id=8));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 7, 'PDP-30000', null, (select id from sponsor where hpms_id=8));

UPDATE user_account SET enabled = false WHERE username = 'EileenCFrierson@example.com';

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t0lsrdZw5uWRx297', null, null, null, (select id from sponsor where hpms_id=2), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t0lsrdZw5uWRx297'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t0lc65ErV8OmY297', null, null, null, (select id from sponsor where hpms_id=3), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t0lc65ErV8OmY297'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t0lkicpxFGkGt297', null, null, null, (select id from sponsor where hpms_id=4), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t0lkicpxFGkGt297'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t0l6c1tQbTikz297', null, null, null, (select id from sponsor where hpms_id=5), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t0l6c1tQbTikz297'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t0lm9qoAtJHqC297', null, null, null, (select id from sponsor where hpms_id=6), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t0lm9qoAtJHqC297'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t0lrjyVeVAZjt297', null, null, null, (select id from sponsor where hpms_id=7), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t0lrjyVeVAZjt297'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0000', 'S0000',
                                                                                           (select id from sponsor where hpms_id=2), '2019-11-01');

UPDATE contract SET sponsor_id = (select id from sponsor where hpms_id = 3) WHERE contract_number = 'S0001';

INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0002', 'S0002',
                                                                                           (select id from sponsor where hpms_id=4), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0005', 'S0005',
                                                                                           (select id from sponsor where hpms_id=5), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0010', 'S0010',
                                                                                           (select id from sponsor where hpms_id=6), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0030', 'S0030',
                                                                                           (select id from sponsor where hpms_id=7), '2019-11-01');
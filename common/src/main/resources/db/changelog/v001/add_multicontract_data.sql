INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 9, 'AB2D-PDP-MULTICONTRACT', null, null);

INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 10, 'AB2D-PDP-S0003', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 11, 'AB2D-PDP-S0004', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 12, 'AB2D-PDP-S0006', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 13, 'AB2D-PDP-S0007', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 14, 'AB2D-PDP-S0008', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 15, 'AB2D-PDP-S0009', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 16, 'AB2D-PDP-S0015', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 17, 'AB2D-PDP-S0100', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 18, 'AB2D-PDP-S0200', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 19, 'AB2D-PDP-S0300', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 20, 'AB2D-PDP-S0400', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 21, 'AB2D-PDP-S0500', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 22, 'AB2D-PDP-S1000', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 23, 'AB2D-PDP-S2000', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 24, 'AB2D-PDP-S3000', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 25, 'AB2D-PDP-S5000', null, (select id from sponsor where hpms_id=9));
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 26, 'AB2D-PDP-S9000', null, (select id from sponsor where hpms_id=9));

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t7wfhvB2qSqPg297', null, null, null, (select id from sponsor where hpms_id=9), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t7wfhvB2qSqPg297'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0oa2t7wdgfB2qSRTg397', null, null, null, (select id from sponsor where hpms_id=10), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa2t7wdgfB2qSRTg397'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0ca27wdgfKefjefTg534', null, null, null, (select id from sponsor where hpms_id=11), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0ca27wdgfKefjefTg534'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0ca27wcvnxkjjefTg334', null, null, null, (select id from sponsor where hpms_id=12), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0ca27wcvnxkjjefTg334'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0ca27w535jfejefTg124', null, null, null, (select id from sponsor where hpms_id=13), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0ca27w535jfejefTg124'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '0ca902fhewffhhhwg144', null, null, null, (select id from sponsor where hpms_id=14), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0ca902fhewffhhhwg144'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '039fuewmn44fhhhwg144', null, null, null, (select id from sponsor where hpms_id=15), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='039fuewmn44fhhhwg144'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '832hfwef93hfkkf3hhhh', null, null, null, (select id from sponsor where hpms_id=16), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='832hfwef93hfkkf3hhhh'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '9fhselhffhf93jhflshh', null, null, null, (select id from sponsor where hpms_id=17), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='9fhselhffhf93jhflshh'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    '39fhklsfzxmneu32feff', null, null, null, (select id from sponsor where hpms_id=18), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='39fhklsfzxmneu32feff'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'bvk329df23fhjfsdjkef', null, null, null, (select id from sponsor where hpms_id=19), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='bvk329df23fhjfsdjkef'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'vdbewfjzjhfew93fh3hh', null, null, null, (select id from sponsor where hpms_id=20), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='vdbewfjzjhfew93fh3hh'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'zvhjefio239fhiew2fff', null, null, null, (select id from sponsor where hpms_id=21), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='zvhjefio239fhiew2fff'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'vxcliwefnl239f03f2jf', null, null, null, (select id from sponsor where hpms_id=22), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='vxcliwefnl239f03f2jf'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'vzdhwfehj3249fwejkfh', null, null, null, (select id from sponsor where hpms_id=23), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='vzdhwfehj3249fwejkfh'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'vznkweicln349fn32rjj', null, null, null, (select id from sponsor where hpms_id=24), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='vznkweicln349fn32rjj'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'xi43hl32sdfl3kfkljew', null, null, null, (select id from sponsor where hpms_id=25), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='xi43hl32sdfl3kfkljew'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));
INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
    'afwjil423fweohfwejkl', null, null, null, (select id from sponsor where hpms_id=26), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='afwjil423fweohfwejkl'),
                                                         (SELECT id FROM role WHERE name='SPONSOR'));

INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0003', 'S0003',
                                                                                           (select id from sponsor where hpms_id=10), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0004', 'S0004',
                                                                                           (select id from sponsor where hpms_id=11), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0006', 'S0006',
                                                                                           (select id from sponsor where hpms_id=12), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0007', 'S0007',
                                                                                           (select id from sponsor where hpms_id=13), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0008', 'S0008',
                                                                                           (select id from sponsor where hpms_id=14), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0009', 'S0009',
                                                                                           (select id from sponsor where hpms_id=15), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0015', 'S0015',
                                                                                           (select id from sponsor where hpms_id=16), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0100', 'S0100',
                                                                                           (select id from sponsor where hpms_id=17), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0200', 'S0200',
                                                                                           (select id from sponsor where hpms_id=18), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0300', 'S0300',
                                                                                           (select id from sponsor where hpms_id=19), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0400', 'S0400',
                                                                                           (select id from sponsor where hpms_id=20), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S0500', 'S0500',
                                                                                           (select id from sponsor where hpms_id=21), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S1000', 'S1000',
                                                                                           (select id from sponsor where hpms_id=22), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S2000', 'S2000',
                                                                                           (select id from sponsor where hpms_id=23), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S3000', 'S3000',
                                                                                           (select id from sponsor where hpms_id=24), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S5000', 'S5000',
                                                                                           (select id from sponsor where hpms_id=25), '2019-11-01');
INSERT INTO contract (id, contract_number, contract_name, sponsor_id, attested_on) VALUES ((select nextval('hibernate_sequence')),'S9000', 'S9000',
                                                                                           (select id from sponsor where hpms_id=26), '2019-11-01');


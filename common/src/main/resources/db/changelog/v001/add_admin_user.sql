--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:add_admin_user failOnError:true
INSERT INTO sponsor (id, hpms_id, org_name, legal_name, parent_id) VALUES ((select nextval('hibernate_sequence')), 9, 'AB2D Admin', null, null);

INSERT INTO user_account (id, username, first_name, last_name, email, sponsor_id, enabled) VALUES ((select nextval('hibernate_sequence')),
                                                                                                   '0oa32rfir2xLtx7s2297', null, null, null, (select id from sponsor where hpms_id=9), true);
INSERT INTO user_role (user_account_id, role_id) VALUES ((SELECT id FROM user_account WHERE username='0oa32rfir2xLtx7s2297'),
                                                         (SELECT id FROM role WHERE name='ADMIN'));

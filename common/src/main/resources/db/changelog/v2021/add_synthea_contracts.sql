--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-3874 failOnError:true

insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1001', 'Z1001', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1002', 'Z1002', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1003', 'Z1003', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1004', 'Z1004', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1005', 'Z1005', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1006', 'Z1006', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1007', 'Z1007', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1008', 'Z1008', '2020-03-01', 'TEST', NOW(), NOW());
insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1009', 'Z1009', '2020-03-01', 'TEST', NOW(), NOW());

insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyx2w9Z0AntLE297', true, 1000, (select id from contract where contract_number='Z1001' limit 1), 'PDP-1001');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jz0e1dyNfRMm6297', true, 1000, (select id from contract where contract_number='Z1002' limit 1), 'PDP-1002');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyzshzgeruw2I297', true, 1000, (select id from contract where contract_number='Z1003' limit 1), 'PDP-1003');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyz6eb9gS4exl297', true, 1000, (select id from contract where contract_number='Z1004' limit 1), 'PDP-1004');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyw49gNqkSfHi297', true, 1000, (select id from contract where contract_number='Z1005' limit 1), 'PDP-1005');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyzq2imADzqGT297', true, 1000, (select id from contract where contract_number='Z1006' limit 1), 'PDP-1006');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyzerqYlfXtsQ297', true, 1000, (select id from contract where contract_number='Z1007' limit 1), 'PDP-1007');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyy4ou5c1MJ3N297', true, 1000, (select id from contract where contract_number='Z1008' limit 1), 'PDP-1008');
insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyw49f9U19vvB297', true, 1000, (select id from contract where contract_number='Z1009' limit 1), 'PDP-1009');

insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyx2w9Z0AntLE297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jz0e1dyNfRMm6297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyzshzgeruw2I297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyz6eb9gS4exl297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyw49gNqkSfHi297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyzq2imADzqGT297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyzerqYlfXtsQ297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyy4ou5c1MJ3N297' limit 1), (select id from role where name='SPONSOR' limit 1));
insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyw49f9U19vvB297' limit 1), (select id from role where name='SPONSOR' limit 1));

ALTER TABLE coverage DETACH PARTITION sandbox;
ALTER TABLE coverage ATTACH PARTITION sandbox FOR VALUES IN
    ('Z0000', 'Z0001', 'Z0002', 'Z0005', 'Z0010', 'Z1001', 'Z1002', 'Z1003', 'Z1004', 'Z1005', 'Z1006', 'Z1007', 'Z1008', 'Z1009');

CREATE TABLE coverage_default PARTITION OF coverage DEFAULT;

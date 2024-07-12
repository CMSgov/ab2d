--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-3874 failOnError:true

insert into contract (id, contract_number, contract_name, attested_on, update_mode, created, modified) values ((select nextval('hibernate_sequence')), 'Z1010', 'Z1010', '2020-03-01', 'TEST', NOW(), NOW());

insert into user_account(id, client_id, enabled, max_parallel_jobs, contract_id, organization) values((select nextval('hibernate_sequence')), '0oa9jyw49f9U20vvB297', true, 1000, (select id from contract where contract_number='Z1010' limit 1), 'PDP-1010');

insert into user_role(user_account_id, role_id) values ((select id from user_account where client_id='0oa9jyw49f9U20vvB297' limit 1), (select id from role where name='ADMIN' limit 1));

ALTER TABLE coverage ATTACH PARTITION sandbox FOR VALUES IN ('Z1010');


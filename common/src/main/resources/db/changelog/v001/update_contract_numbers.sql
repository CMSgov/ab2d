--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:update_contract_numbers failOnError:true
UPDATE contract SET contract_number = 'Z0000', contract_name = 'Z0000' WHERE contract_number = 'S0000';
UPDATE contract SET contract_number = 'Z0001', contract_name = 'Z0001' WHERE contract_number = 'S0001';
UPDATE contract SET contract_number = 'Z0002', contract_name = 'Z0002' WHERE contract_number = 'S0002';
UPDATE contract SET contract_number = 'Z0003', contract_name = 'Z0003' WHERE contract_number = 'S0003';
UPDATE contract SET contract_number = 'Z0004', contract_name = 'Z0004' WHERE contract_number = 'S0004';
UPDATE contract SET contract_number = 'Z0005', contract_name = 'Z0005' WHERE contract_number = 'S0005';
UPDATE contract SET contract_number = 'Z0006', contract_name = 'Z0006' WHERE contract_number = 'S0006';
UPDATE contract SET contract_number = 'Z0007', contract_name = 'Z0007' WHERE contract_number = 'S0007';
UPDATE contract SET contract_number = 'Z0008', contract_name = 'Z0008' WHERE contract_number = 'S0008';
UPDATE contract SET contract_number = 'Z0009', contract_name = 'Z0009' WHERE contract_number = 'S0009';
UPDATE contract SET contract_number = 'Z0010', contract_name = 'Z0010' WHERE contract_number = 'S0010';
UPDATE contract SET contract_number = 'Z0015', contract_name = 'Z0015' WHERE contract_number = 'S0015';
UPDATE contract SET contract_number = 'Z0030', contract_name = 'Z0030' WHERE contract_number = 'S0030';
UPDATE contract SET contract_number = 'Z0100', contract_name = 'Z0100' WHERE contract_number = 'S0100';
UPDATE contract SET contract_number = 'Z0200', contract_name = 'Z0200' WHERE contract_number = 'S0200';
UPDATE contract SET contract_number = 'Z0300', contract_name = 'Z0300' WHERE contract_number = 'S0300';
UPDATE contract SET contract_number = 'Z0400', contract_name = 'Z0400' WHERE contract_number = 'S0400';
UPDATE contract SET contract_number = 'Z0500', contract_name = 'Z0500' WHERE contract_number = 'S0500';
UPDATE contract SET contract_number = 'Z1000', contract_name = 'Z1000' WHERE contract_number = 'S1000';
UPDATE contract SET contract_number = 'Z2000', contract_name = 'Z2000' WHERE contract_number = 'S2000';
UPDATE contract SET contract_number = 'Z3000', contract_name = 'Z3000' WHERE contract_number = 'S3000';
UPDATE contract SET contract_number = 'Z5000', contract_name = 'Z5000' WHERE contract_number = 'S5000';
UPDATE contract SET contract_number = 'Z9000', contract_name = 'Z9000' WHERE contract_number = 'S9000';
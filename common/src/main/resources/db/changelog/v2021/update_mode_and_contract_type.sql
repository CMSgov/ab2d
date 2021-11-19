--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:updatae_mode_and_contract_type failOnError:true

ALTER TABLE contract ADD contract_type varchar(255) NOT NULL DEFAULT 'NORMAL';
UPDATE contract SET contract_type = 'CLASSIC_TEST' where update_mode = 'TEST';
UPDATE contract SET update_mode = 'NONE' where update_mode = 'TEST';
UPDATE contract SET contract_type = 'SYNTHEA' where contract_number LIKE 'Z1%';

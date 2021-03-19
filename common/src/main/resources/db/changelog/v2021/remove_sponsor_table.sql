--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:remove_sponsor_table failOnError:true

ALTER TABLE contract DROP CONSTRAINT fk_contract_to_sponsor;
ALTER TABLE contract DROP COLUMN sponsor_id;

ALTER TABLE user_account DROP CONSTRAINT fk_user_account_to_sponsor;
ALTER TABLE user_account DROP COLUMN sponsor_id;

ALTER TABLE sponsor_ip DROP CONSTRAINT fk_sponsor_ip_to_sponsor;
DROP TABLE sponsor_ip;

DROP TABLE sponsor;
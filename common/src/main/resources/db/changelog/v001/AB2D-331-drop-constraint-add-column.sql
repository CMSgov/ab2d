--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:AB2D-331-DropUCSponsorHMPSIDConstraint failOnError:true dbms:postgresql
ALTER TABLE sponsor DROP CONSTRAINT "uc_sponsor_hpms_id";

--changeset adaykin:AB2D-331-AddContractName failOnError:true dbms:postgresql
ALTER TABLE contract ADD COLUMN contract_name VARCHAR(128) NOT NULL;
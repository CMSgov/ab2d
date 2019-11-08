--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:AB2D-333-AddConstraint failOnError:true dbms:postgresql
ALTER TABLE contract ADD COLUMN sponsor_id BIGINT NOT NULL;
ALTER TABLE contract ADD CONSTRAINT "fk_contract_to_sponsor" FOREIGN KEY (sponsor_id) REFERENCES sponsor (id);
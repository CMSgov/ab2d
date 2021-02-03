--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:add_fhir_version failOnError:true

ALTER TABLE job ADD COLUMN fhir_version varchar(255);
UPDATE job SET fhir_version = 'STU3' WHERE fhir_version IS NULL;
ALTER TABLE job ALTER COLUMN fhir_version SET NOT NULL;
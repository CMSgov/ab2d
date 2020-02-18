--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:ab2d-934-allow-for-zip-downloads failOnError:true
ALTER TABLE job ADD COLUMN output_format VARCHAR(255);

--rollback  ALTER TABLE job DROP COLUMN IF EXISTS output_format;

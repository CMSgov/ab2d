--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:ab2d-777-opt-out-resolve-ben-id-mbi failOnError:true
ALTER TABLE opt_out ADD COLUMN mbi VARCHAR(255);
ALTER TABLE opt_out ADD COLUMN ccw_id VARCHAR(255);

--rollback  ALTER TABLE opt_out DROP COLUMN IF EXISTS mbi;
--rollback  ALTER TABLE opt_out DROP COLUMN IF EXISTS ccw_id;

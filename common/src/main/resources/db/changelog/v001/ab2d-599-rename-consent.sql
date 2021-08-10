--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:ab2d-599-rename-consent-table-to-opt_out failOnError:true
ALTER TABLE consent RENAME TO opt_out;
ALTER TABLE opt_out RENAME CONSTRAINT "pk_consent" TO "pk_opt_out";
ALTER INDEX "ix_consent_hicn" RENAME TO "ix_opt_out_hicn";

--rollback  ALTER TABLE opt_out RENAME TO consent;
--rollback  ALTER TABLE consent RENAME CONSTRAINT "pk_opt_out" TO "pk_consent";
--rollback  ALTER INDEX "ix_opt_out_hicn" RENAME TO "ix_consent_hicn";
--  -------------------------------------------------------------------------------------------------------------------
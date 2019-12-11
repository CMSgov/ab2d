--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:AB2D-514-CreateTable-opt_out failOnError:true
ALTER TABLE consent RENAME TO opt_out;
--ALTER TABLE opt_out RENAME CONSTRAINT pk_consent TO pk_opt_out;
--ALTER TABLE opt_out RENAME CONSTRAINT ix_consent_hicn TO ix_opt_out_hicn;

--rollback  ALTER TABLE opt_out RENAME TO consent;

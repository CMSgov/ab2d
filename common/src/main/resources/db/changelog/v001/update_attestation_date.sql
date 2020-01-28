--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:AB2D-747-filter-out-claims-on-dates
UPDATE contract SET attested_on='2000-01-01';

--rollback UPDATE contract SET attested_on='2019-11-01';

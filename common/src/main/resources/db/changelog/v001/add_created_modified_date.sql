--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset enolan:add_created_modified_date failOnError:true
ALTER TABLE beneficiary
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE beneficiary SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE contract
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE contract SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE coverage
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE coverage SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE opt_out
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE opt_out SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE opt_out_file
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE opt_out_file SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE properties
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE properties SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE role
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE role SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE sponsor
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE sponsor SET created='2000-01-01', modified='2000-01-01';

ALTER TABLE user_account
    ADD COLUMN created timestamp,
    ADD COLUMN modified timestamp;

-- noinspection SqlWithoutWhere
UPDATE user_account SET created='2000-01-01', modified='2000-01-01';


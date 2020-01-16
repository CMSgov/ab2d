--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------


--changeset spathiyil:ab2d-828-increase-username-column-length failOnError:true
ALTER TABLE user_account ALTER COLUMN username TYPE VARCHAR(255)

--rollback  ALTER TABLE user_account ALTER COLUMN username TYPE VARCHAR(64)
--  -------------------------------------------------------------------------------------------------------------------



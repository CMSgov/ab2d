--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_alias_user_accounts failOnError:true

ALTER TABLE user_account ADD COLUMN alias VARCHAR(255)
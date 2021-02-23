--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:rename_username_user_account failOnError:true

ALTER TABLE user_account RENAME username TO client_id;
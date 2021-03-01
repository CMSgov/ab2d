--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:rework_user_account_username failOnError:true

ALTER TABLE user_account ADD COLUMN organization VARCHAR(255) NOT NULL DEFAULT '';

UPDATE user_account
SET organization = sponsor.org_name
FROM sponsor
WHERE sponsor.id = user_account.sponsor_id;

ALTER TABLE user_account ADD UNIQUE (organization);

ALTER TABLE user_account DROP COLUMN first_name;
ALTER TABLE user_account DROP COLUMN last_name;
ALTER TABLE user_account DROP COLUMN email;

ALTER TABLE user_account RENAME username TO client_id;
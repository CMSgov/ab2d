--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:remove_current_admin failOnError:true

DELETE FROM user_role
WHERE user_account_id = (SELECT id FROM user_account WHERE client_id = '0oa32rfir2xLtx7s2297');

DELETE FROM user_account
WHERE client_id = '0oa32rfir2xLtx7s2297';
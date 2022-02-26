--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset enolan:contract_bus_key failOnError:true

-- -- Create and populate the contract business key
-- ALTER TABLE user_account ADD COLUMN contract_number VARCHAR(255);
-- UPDATE user_account
-- SET contract_number = c.contract_number
-- FROM contract c
-- WHERE contract_id = c.id;
--
-- -- Enforce that a saved user_account has a contract number
-- ALTER TABLE user_account ALTER "contract_number" SET NOT NULL;
-- --
-- -- -- Foreign key is no longer needed.  The business key will be used going forward
-- ALTER TABLE user_account DROP CONSTRAINT fk_user_to_contract;
-- ALTER TABLE user_account DROP COLUMN contract_id CASCADE;


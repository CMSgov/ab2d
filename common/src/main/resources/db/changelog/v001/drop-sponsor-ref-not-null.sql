ALTER TABLE contract ALTER COLUMN sponsor_id DROP NOT NULL;
ALTER TABLE user_account ALTER COLUMN sponsor_id DROP NOT NULL;
ALTER TABLE user_account ALTER COLUMN contract_id DROP NOT NULL;


ALTER TABLE user_account ADD CONSTRAINT "fk_user_to_contract" FOREIGN KEY (contract_id) REFERENCES contract (id);

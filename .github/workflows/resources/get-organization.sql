SELECT d.contract_name FROM contract.contract d
INNER JOIN ${DATABASE_SCHEMA_NAME}.user_account e ON d.id = e.contract_id
WHERE d.contract_number = '${CONTRACT_NUMBER}';
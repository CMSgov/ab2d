SELECT d.organization FROM ${DATABASE_SCHEMA_NAME}.user_account d
INNER JOIN contract.contract  e ON e.id = d.contract_id
WHERE e.contract_number = '${CONTRACT_NUMBER}';
SELECT DISTINCT b.id, b.contract_number, b.contract_name, b.sponsor_id, b.attested_on
FROM contract b
LEFT OUTER JOIN user_account c
ON b.id = c.contract_id
WHERE b.contract_number NOT LIKE 'Z%'
AND b.attested_on IS NOT NULL
AND c.contract_id IS NULL
ORDER BY 1;

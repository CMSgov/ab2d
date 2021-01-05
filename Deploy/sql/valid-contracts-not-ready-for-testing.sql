SELECT
  contract_number,
  contract_name,
  sponsor_id,
  attested_on
FROM public.contract
WHERE attested_on IS NOT NULL
AND contract_number NOT LIKE 'Z%'
AND contract_number NOT IN (
  SELECT
    d.contract_number
  FROM public.contract d
  INNER JOIN public.user_account e
  ON d.id = e.contract_id
  WHERE d.attested_on is not null
  AND d.contract_number NOT LIKE 'Z%'
  AND e.enabled = true
  AND e.username IN (
    SELECT username
    FROM (
      SELECT
        e.username, COUNT(e.username) AS username_count
      FROM public.contract d
      INNER JOIN public.user_account e
      ON d.id = e.contract_id
      WHERE d.attested_on is not null
      AND d.contract_number NOT LIKE 'Z%'
      AND e.enabled = true
      GROUP BY e.username
      HAVING COUNT(e.username) <= 1
    ) valid_users
  )
)
ORDER BY 1;


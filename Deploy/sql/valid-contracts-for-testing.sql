SELECT
  d.contract_number AS "Contract Number",
  d.contract_name AS "Contract Name",
  'Attested' AS "Attestation Status",
  cast(d.attested_on as date) AS "Start Date",
  'N/A' AS "End Date",
  concat(e.first_name, ' ', e.last_name) AS "Attesting User",
  e.username,
  e.email,
  e.enabled,
  e.sponsor_id,
  e.max_parallel_jobs
FROM public.sponsor a
INNER JOIN public.contract d
ON a.id = d.sponsor_id
INNER JOIN public.user_account e
ON a.id = e.sponsor_id
WHERE d.attested_on is not null
AND d.contract_number NOT LIKE 'Z%'
AND e.enabled = true
AND e.username IN (
  SELECT username
  FROM (
    SELECT
      e.username, COUNT(e.username) AS username_count
    FROM public.sponsor a
    INNER JOIN public.contract d
    ON a.id = d.sponsor_id
    INNER JOIN public.user_account e
    ON a.id = e.sponsor_id
    WHERE d.attested_on is not null
    AND d.contract_number NOT LIKE 'Z%'
    AND e.enabled = true
    GROUP BY e.username
    HAVING COUNT(e.username) <= 1
  ) valid_users
)
ORDER BY d.contract_number, e.username;

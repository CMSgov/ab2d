SELECT
  d.contract_number AS "Contract Number",
  d.contract_name AS "Contract Name",
  extract(hour from AGE(f.completed_at, f.created_at)) || ' hour(s), '
    || extract(minute from AGE(f.completed_at, f.created_at)) || ' minute(s), and '
	|| round(extract (second from AGE(f.completed_at, f.created_at))) || ' second(s)'
  AS job_time,
  f.created_at,
  f.completed_at,
  f.status,
  f.progress,
  f.job_uuid
FROM public.contract d
INNER JOIN public.user_account e
ON d.id = e.contract_id
INNER JOIN public.job f
ON e.id = f.user_account_id
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
ORDER BY d.contract_number, f.completed_at

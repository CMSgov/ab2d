SELECT
  f.created_at,
  f.completed_at,
  a.org_name,
  f.status,
  f.progress,
  f.job_uuid,
  f.user_account_id,
  f.contract_id
FROM job f
INNER JOIN user_account e
ON f.user_account_id = e.id
INNER JOIN sponsor a
ON e.sponsor_id = a.id
ORDER BY 1 DESC;

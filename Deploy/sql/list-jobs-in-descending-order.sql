SELECT created_at, completed_at, org_name, status, progress, job_uuid, user_account_id
FROM job
INNER JOIN user_account
ON job.user_account_id = user_account.id
INNER JOIN sponsor
ON user_account.sponsor_id = sponsor.id
ORDER BY 1 DESC;

SELECT DISTINCT
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
FROM public.contract d
INNER JOIN public.user_account e
ON d.id = e.contract_id
WHERE d.attested_on is not null
ORDER BY d.contract_number, e.username;

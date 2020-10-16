SELECT
  a.org_name AS parent_org
  , b.org_name AS org
  , c.ip_address
FROM public.sponsor a
INNER JOIN public.sponsor b
ON a.id = b.parent_id
INNER JOIN public.sponsor_ip c
ON b.id = c.sponsor_id
ORDER BY parent_org, org, ip_address;
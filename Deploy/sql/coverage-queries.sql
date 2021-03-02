-------
-- role
-------

SELECT MIN(id)
FROM public.role;

-- 1

SELECT id, name, created, modified
FROM public.role
ORDER BY 1
LIMIT 5;

----------
-- sponsor
----------

SELECT MIN(id)
FROM public.sponsor;

-- 3

SELECT id, hpms_id, org_name, legal_name, parent_id, created, modified
FROM public.sponsor
ORDER BY 1
LIMIT 1;

-----------
-- contract
-----------

SELECT MIN(id)
FROM public.contract;

-- 6

SELECT id, contract_number, contract_name, sponsor_id, attested_on, created, modified, hpms_parent_org_id, hpms_parent_org_name, hpms_org_marketing_name, update_mode
FROM public.contract
ORDER BY 1
LIMIT 5;

---------------
-- user_account
---------------

SELECT MIN(id)
FROM public.user_account;

-- 14

SELECT id, username, first_name, last_name, email, sponsor_id, enabled, max_parallel_jobs, created, modified, contract_id
FROM public.user_account
ORDER BY 1
LIMIT 5;

-----------------------------------------------------
-- bene_coverage_period (bene_coverage_period_id_seq)
-----------------------------------------------------

SELECT MIN(id)
FROM public.bene_coverage_period;

-- 3

SELECT id, contract_id, month, year, status, created, modified, last_successful_job
FROM public.bene_coverage_period
ORDER BY 1
LIMIT 5;

---------------------------------------------------------------------------------------------
-- event_bene_coverage_search_status_change (event_bene_coverage_search_status_change_id_seq)
---------------------------------------------------------------------------------------------

SELECT MIN(id)
FROM public.event_bene_coverage_search_status_change;

-- 5

SELECT id, bene_coverage_period_id, old_status, new_status, created, modified, description
FROM public.event_bene_coverage_search_status_change
ORDER BY 1
LIMIT 5;

-----------------------------
-- coverage (coverage_id_seq)
-----------------------------

SELECT MIN(id)
FROM public.coverage;

-- 1

SELECT id, bene_coverage_period_id, bene_coverage_search_event_id, beneficiary_id, current_mbi, historic_mbis
FROM public.coverage
ORDER BY 1
LIMIT 5;





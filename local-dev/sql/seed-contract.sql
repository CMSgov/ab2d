-- Local-dev seed for contract and PdpClient

BEGIN;

INSERT INTO contract.contract (
    id, contract_number, contract_name, hpms_parent_org_id, hpms_parent_org_name,
    hpms_org_marketing_name, update_mode, contract_type, total_enrollment,
    medicare_eligible, attested_on, created, modified
)
VALUES (
    nextval('contract.contract_seq'), 'Z0001', 'Local Test Contract', 1000,
    'Local Test Org', 'Local Test Org Marketing', 'AUTOMATIC', 'NORMAL', 100, 100,
    '2020-03-01 12:00:00+00', NOW(), NOW()
)
ON CONFLICT (contract_number) DO NOTHING;

-- SPONSOR role.
INSERT INTO ab2d.role (id, name, created, modified)
VALUES (nextval('ab2d.role_seq'), 'SPONSOR', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- PdpClient (user_account). The contract_id references contract.contract.id.
INSERT INTO ab2d.user_account (
    id, client_id, organization, enabled, contract_id, max_parallel_jobs, created, modified
)
SELECT
    nextval('ab2d.user_account_seq'),
    'EileenCFrierson@example.com',
    'Local Test Org',
    TRUE,
    c.id,
    3,
    NOW(),
    NOW()
FROM contract.contract c
WHERE c.contract_number = 'Z0001'
ON CONFLICT (client_id) DO NOTHING;

-- Link the user to the SPONSOR role.
INSERT INTO ab2d.user_role (user_account_id, role_id)
SELECT u.id, r.id
FROM ab2d.user_account u, ab2d.role r
WHERE u.client_id = 'EileenCFrierson@example.com' AND r.name = 'SPONSOR'
ON CONFLICT (user_account_id, role_id) DO NOTHING;

COMMIT;

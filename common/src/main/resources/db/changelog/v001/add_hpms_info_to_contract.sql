ALTER TABLE contract
    ADD COLUMN hpms_parent_org_id BIGINT,
    ADD COLUMN hpms_parent_org_name VARCHAR(255),
    ADD COLUMN hpms_org_marketing_name VARCHAR(255);

INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'hpms.ingest.engaged', 'engaged');
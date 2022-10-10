CREATE SCHEMA IF NOT EXISTS property;

CREATE TABLE property.properties (
    id BIGINT NOT NULL,
    key VARCHAR(80) NOT NULL,
    value VARCHAR(255) NOT NULL,
    created timestamp,
    modified timestamp
);

ALTER TABLE property.properties ADD CONSTRAINT "uc_properties_key" UNIQUE (key);
ALTER TABLE property.properties ADD CONSTRAINT "pk_properties" PRIMARY KEY (id);

CREATE SEQUENCE IF NOT EXISTS property.property_sequence
    AS integer OWNED BY property.properties.id;
SELECT setval('property.property_sequence', max(id)) FROM property.properties;

INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'pcp.core.pool.size', 10, now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'pcp.max.pool.size', 150, now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'pcp.scaleToMax.time', 800, now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'maintenance.mode', 'false', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'worker.engaged', 'engaged', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'hpms.ingest.engaged', 'engaged', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'coverage.update.discovery', 'idle', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'coverage.update.queueing', 'idle', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'coverage.update.months.past', '1', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'coverage.update.stuck.hours', '24', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'ZipSupportOn', 'false', now(), now());
INSERT INTO property.properties (id, key, value, created, modified) VALUES((select nextval('property.property_sequence')), 'coverage.update.override', 'false', now(), now());

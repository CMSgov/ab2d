--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:add_properties_table failOnError:true
CREATE TABLE properties (
    id BIGINT NOT NULL,
    key VARCHAR(80) NOT NULL,
    value VARCHAR(255) NOT NULL
);

ALTER TABLE properties ADD CONSTRAINT "uc_properties_key" UNIQUE (key);
ALTER TABLE properties ADD CONSTRAINT "pk_properties" PRIMARY KEY (id);

INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'pcp.core.pool.size', 10);
INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'pcp.max.pool.size', 150);
INSERT INTO properties (id, key, value) VALUES((select nextval('hibernate_sequence')), 'pcp.scaleToMax.time', 900);

--rollback  DROP TABLE properties;
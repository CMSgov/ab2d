--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:add_coverage_search_table failOnError:true
CREATE TABLE coverage_search (
    id BIGINT NOT NULL,
    bene_coverage_period_id INTEGER NOT NULL,
    created timestamp
);

ALTER TABLE coverage_search ADD CONSTRAINT "pk_coverage_search" PRIMARY KEY (id);
ALTER TABLE coverage_search ADD CONSTRAINT "fk_coverage_search_bene_coverage_period"
    FOREIGN KEY (bene_coverage_period_id) REFERENCES bene_coverage_period(id);

--rollback  DROP TABLE coverage_search;

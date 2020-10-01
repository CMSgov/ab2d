--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset lsharshar:add_coverage_search_table failOnError:true
CREATE TABLE coverage_search (
    id BIGINT NOT NULL,
    contract VARCHAR(80) NOT NULL,
    month integer NOT NULL,
    year integer NOT NULL,
    created timestamp,
    PRIMARY KEY (id)
);

--rollback  DROP TABLE coverage_search;

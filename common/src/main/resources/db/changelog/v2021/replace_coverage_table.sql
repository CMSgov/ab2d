--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset sb-wnyffenegger:replace_coverage_table failOnError:true

ALTER TABLE coverage RENAME TO coverage_deprecated;

ALTER TABLE coverage_partitioned RENAME TO coverage;


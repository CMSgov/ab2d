--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset sb-wnyffenegger:drop_old_coverage_table failOnError:true

DROP TABLE coverage_deprecated;


--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:remove_deprecated_properties failOnError:true

DELETE FROM properties
WHERE key IN ('coverage.update.stale.days', 'ContractToBeneCachingOn');
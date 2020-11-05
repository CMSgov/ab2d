--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset wnyffenegger:add_mbi_to_coverage failOnError:true

-- All previous coverage information is now useless
DELETE FROM coverage;

-- Secondary identifier that must be reported
ALTER TABLE coverage ADD COLUMN "mbi" VARCHAR(32) NOT NULL;

-- These two indexes are used for index only searches of the coverage table (B-Tree in memory instead of table on disk)
-- While these indexes slow down insertions they are absolutely vital for the selects (1 minute for a select to 1 second)

-- Adding mbi onto these indexes as an in memory search
DROP INDEX "ix_coverage_period_beneficiary_id_index";
DROP INDEX "ix_coverage_period_beneficiary_id_index_inverted";
CREATE INDEX "ix_coverage_period_beneficiary_id_index" ON coverage(bene_coverage_period_id, beneficiary_id) INCLUDE (mbi);
CREATE INDEX "ix_coverage_period_beneficiary_id_index_inverted" ON coverage(beneficiary_id, bene_coverage_period_id) INCLUDE (mbi);
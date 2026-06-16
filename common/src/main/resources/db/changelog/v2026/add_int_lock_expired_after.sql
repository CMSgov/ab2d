--liquibase formatted sql

--changeset system:add-int-lock-expired-after
--preconditions onFail:MARK_RAN onError:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT count(*) FROM pg_tables WHERE tablename = 'int_lock' AND schemaname = current_schema()
ALTER TABLE int_lock ADD COLUMN IF NOT EXISTS expired_after TIMESTAMP NOT NULL DEFAULT NOW();

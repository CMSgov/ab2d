--liquibase formatted sql
--  -------------------------------------------------------------------------------------------------------------------

--changeset adaykin:add_concurrent_job_column failOnError:true
ALTER TABLE user_account ADD COLUMN max_parallel_jobs INT DEFAULT 1 NOT NULL;

UPDATE user_account SET max_parallel_jobs = 3;

UPDATE user_account SET max_parallel_jobs = 30 WHERE username = '0oa2t7wfhvB2qSqPg297';
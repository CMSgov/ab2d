create extension if not exists pg_cron;

-- run job every Tuesday at 7:00 am
--SELECT cron.schedule ('update_current_mbis','0 7 * * 2','call update_current_mbi_2023()');

-- job that runs every day at midnight to purge the cron.job_run_details table. The job keeps only the last seven days.
--SELECT cron.schedule('0 0 * * *', $$DELETE
--    FROM cron.job_run_details
--    WHERE end_time < now() - interval '7 days'$$);
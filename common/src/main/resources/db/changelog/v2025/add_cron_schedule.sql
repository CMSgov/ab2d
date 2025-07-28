TRUNCATE TABLE cron.job;

SELECT cron.schedule('0 7 * * *', 'CALL proc_insert_mbi_to_table(0,40);');
SELECT cron.schedule('20 7 * * *', 'CALL proc_insert_mbi_to_table(41,20);');
SELECT cron.schedule('0 8 * * *', 'CALL proc_insert_mbi_to_table(81,100);');
SELECT cron.schedule('40 7 * * *', 'CALL proc_insert_mbi_to_table(61,12);');
SELECT cron.schedule('10 8 * * *', 'CALL proc_insert_mbi_to_table(74,6);');

SELECT cron.schedule('delete-job-run-details','0 12 * * *',$$DELETE FROM cron.job_run_details WHERE end_time < now() - interval '7 days'$$);

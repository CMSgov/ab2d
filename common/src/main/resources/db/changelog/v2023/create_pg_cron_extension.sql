-- run job every night at 1:00 am
SELECT cron.schedule ('update_current_mbis','0 1 * * *','call update_current_mbi()');
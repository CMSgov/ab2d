-- NEEDED to reload config after installing pg_cron
SELECT pg_reload_conf();

CREATE EXTENSION if not exists pg_cron;

-- run job every night at 1:00 am
SELECT cron.schedule ('update_current_mbis','0 */2 * * *','call update_current_mbi()');
-- Cooldown marker for the pause/resume prototype's stranded-job alert.
-- Stamped when a worker claims the right to alert about an unrecovered lease, so that many workers polling
-- on a short interval produce one alert per job per cooldown instead of one per worker per poll.
ALTER TABLE ab2d.job_lease ADD COLUMN IF NOT EXISTS alerted_at TIMESTAMPTZ;

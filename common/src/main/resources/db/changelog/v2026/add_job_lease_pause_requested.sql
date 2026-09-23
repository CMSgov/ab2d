-- manual pause request for a specific job
ALTER TABLE ab2d.job_lease ADD COLUMN IF NOT EXISTS pause_requested BOOLEAN NOT NULL DEFAULT FALSE;

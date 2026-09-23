-- boolean column to decide if a job should be processed by the prototype
ALTER TABLE ab2d.job ADD COLUMN IF NOT EXISTS pause_eligible BOOLEAN NOT NULL DEFAULT FALSE;

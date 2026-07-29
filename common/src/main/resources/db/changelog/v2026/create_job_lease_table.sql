-- Ownership lease for the pause/resume prototype's crash recovery.
-- Theoretically could replace the current INT_LOCK
CREATE TABLE IF NOT EXISTS ab2d.job_lease (
    job_uuid            VARCHAR(255) PRIMARY KEY,
    owner               VARCHAR(255),
    token               BIGINT      NOT NULL DEFAULT 0,
    heartbeat_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    clean_suspend_token BIGINT
);

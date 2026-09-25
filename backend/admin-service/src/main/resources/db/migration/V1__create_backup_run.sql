CREATE TABLE backup_run (
    id UUID PRIMARY KEY,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL,
    size_bytes BIGINT,
    object_key VARCHAR(255),
    triggered_by UUID,
    error_message TEXT
);

CREATE INDEX idx_backup_run_started_at ON backup_run (started_at DESC);

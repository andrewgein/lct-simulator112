ALTER TABLE incidents ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_incidents_available ON incidents (target_type, difficulty) WHERE deleted = FALSE;

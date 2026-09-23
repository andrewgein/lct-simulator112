ALTER TABLE contexts RENAME COLUMN level_id TO assignment_id;
CREATE INDEX idx_contexts_assignment ON contexts (assignment_id);

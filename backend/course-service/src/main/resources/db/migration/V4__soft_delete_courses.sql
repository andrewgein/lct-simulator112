ALTER TABLE courses ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_courses_author_deleted_at ON courses (author_id, deleted_at);

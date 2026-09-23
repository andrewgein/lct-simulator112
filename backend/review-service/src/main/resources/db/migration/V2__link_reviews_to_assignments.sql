ALTER TABLE reviews RENAME COLUMN level_id TO assignment_id;
CREATE INDEX idx_reviews_assignment ON reviews (assignment_id);

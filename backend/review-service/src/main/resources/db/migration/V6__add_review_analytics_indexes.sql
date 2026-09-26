CREATE INDEX idx_reviews_analytics_access_created
    ON reviews (user_id, assignment_id, created_at DESC);

CREATE INDEX idx_criterion_results_analytics
    ON criterion_results (incident_id, criterion_name, review_id);

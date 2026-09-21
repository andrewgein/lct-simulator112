CREATE TABLE reviews (
    context_id UUID PRIMARY KEY,
    user_id UUID,
    level_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_reviews_user_created_at
    ON reviews (user_id, created_at DESC);

CREATE TABLE criterion_results (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL,
    incident_id VARCHAR(255) NOT NULL,
    incident_order INTEGER NOT NULL,
    criterion_name VARCHAR(255) NOT NULL,
    score INTEGER NOT NULL,
    max_score INTEGER NOT NULL,
    feedback TEXT NOT NULL,
    CONSTRAINT fk_criterion_results_review
        FOREIGN KEY (review_id) REFERENCES reviews (context_id) ON DELETE CASCADE,
    CONSTRAINT ck_criterion_result_score
        CHECK (score >= 0 AND max_score >= 0 AND score <= max_score)
);

CREATE INDEX idx_criterion_results_review
    ON criterion_results (review_id, incident_order);

CREATE TABLE reviews (
    context_id UUID PRIMARY KEY,
    user_id UUID,
    level_id VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    review TEXT,
    incident_data TEXT,
    filled_data TEXT,
    dialog_data TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABlE criterion_results (
    id UUID PRIMARY KEY,
    review_id UUID,
    criterion_name VARCHAR(255),
    score INTEGER,
    max_score INTEGER,
    feedback TEXT
);
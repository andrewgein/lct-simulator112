CREATE TABLE cached_recommendations (
    user_id UUID PRIMARY KEY,
    recommendations TEXT NOT NULL,
    reviews_count INTEGER NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

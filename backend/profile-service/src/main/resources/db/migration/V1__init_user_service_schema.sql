CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE
);

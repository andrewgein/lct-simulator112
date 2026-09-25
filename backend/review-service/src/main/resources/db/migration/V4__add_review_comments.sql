CREATE TABLE review_comments (
    id UUID PRIMARY KEY,
    review_context_id UUID NOT NULL,
    author_id UUID NOT NULL,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_review_comments_review
        FOREIGN KEY (review_context_id) REFERENCES reviews (context_id) ON DELETE CASCADE,
    CONSTRAINT ck_review_comment_text
        CHECK (CHAR_LENGTH(TRIM(comment_text)) BETWEEN 1 AND 4000)
);

CREATE INDEX idx_review_comments_review_created_at
    ON review_comments (review_context_id, created_at);

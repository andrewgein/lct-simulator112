ALTER TABLE messages
    ADD CONSTRAINT uk_messages_event_id UNIQUE (event_id);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    text TEXT NOT NULL,
    target_url VARCHAR(1024) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    email_status VARCHAR(32) NOT NULL,
    email_sent_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_email_status
        CHECK (email_status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT ck_notifications_type
        CHECK (type IN ('REVIEW_COMMENT'))
);

CREATE INDEX idx_notifications_user_created_at
    ON notifications (user_id, created_at DESC);

CREATE INDEX idx_notifications_user_read_at
    ON notifications (user_id, read_at);

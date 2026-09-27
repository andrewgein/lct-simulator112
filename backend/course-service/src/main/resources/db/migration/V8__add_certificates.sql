CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    course_title VARCHAR(255) NOT NULL,
    percent INTEGER NOT NULL,
    type VARCHAR(32) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_certificates_user_course UNIQUE (user_id, course_id),
    CONSTRAINT ck_certificates_percent CHECK (percent >= 0 AND percent <= 100),
    CONSTRAINT ck_certificates_type CHECK (type IN ('COMPLETION', 'HONORS'))
);
CREATE INDEX idx_certificates_user_issued_at ON certificates (user_id, issued_at DESC);

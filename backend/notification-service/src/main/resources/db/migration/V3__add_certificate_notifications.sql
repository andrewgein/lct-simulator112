ALTER TABLE notifications
    DROP CONSTRAINT ck_notifications_type;

ALTER TABLE notifications
    ADD CONSTRAINT ck_notifications_type
        CHECK (type IN ('REVIEW_COMMENT', 'CERTIFICATE_ISSUED'));

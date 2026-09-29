-- In-app уведомления не зависят от локальной копии users (она заполняется только событием user.created)
ALTER TABLE notifications
    DROP CONSTRAINT fk_notifications_user;

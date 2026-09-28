# notification-service

## Описание

Слушает события Kafka (`email.verification.requested`, `password.reset.requested`, `user.created`, `review.comment.created`, `certificate.issued`), создаёт уведомления пользователей и отправляет письма через SMTP (Gmail). Уведомления отдаёт через REST. Порт HTTP: `8090`.

## Локальный запуск

Нужны: PostgreSQL (база `notification`), Kafka и Gmail-аккаунт с паролем приложения.

```bash
export MAIL_USERNAME=...
export MAIL_PASSWORD=...
mvn spring-boot:run
```

Maven-wrapper в каталоге нет, нужен установленный `mvn`. Остальные переменные: `SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `APP_BASE_URL`.

# auth-service

## Описание

Регистрация, вход, выпуск и обновление JWT (подпись RSA), сброс пароля, подтверждение почты и роли пользователей. Приватный ключ есть только в этом сервисе. Порт HTTP: `8081`.

## Локальный запуск

Нужны: PostgreSQL (база `auth`), Kafka и RSA-ключ (`private.pem`).

```bash
export DB_PASSWORD=...
export JWT_PRIVATE_KEY_PATH=/path/to/private.pem
./mvnw spring-boot:run
```

Остальные переменные (`DB_URL`, `DB_USERNAME`, `KAFKA_BOOTSTRAP_SERVERS`, `FRONTEND_BASE_URL`) имеют значения по умолчанию для localhost. Образец — `.env.example`.

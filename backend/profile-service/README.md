# profile-service

## Описание

Профили пользователей: имя, фамилия и профессиональный профиль (направление обучения — оператор 112 или ДДС — и код службы ДДС). Отдаёт профиль по `X-User-Id` или по id, по gRPC — другим сервисам. Порты: HTTP `8082`, gRPC `9094`. Swagger: `http://localhost:8082/swagger-ui/index.html`.

## Локальный запуск

Нужны: PostgreSQL (база `profile`, миграции Flyway применяются сами) и classifier-service по gRPC (`9093`).

```bash
./mvnw spring-boot:run
```

Переменные при необходимости: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `GRPC_SERVER_PORT`, `CLASSIFIER_GRPC_HOST`, `CLASSIFIER_GRPC_PORT`.

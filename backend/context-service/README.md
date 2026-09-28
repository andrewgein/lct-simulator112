# context-service

## Описание

Хранит состояние конкретного прохождения уровня (SYSTEM_112 или DDS): очередь звонков, транскрипты, карточки решений, этапы и дедлайны DDS. По завершении отправляет данные в review-service. Порты: HTTP `8084`, gRPC `9090`.

## Локальный запуск

Нужны: PostgreSQL (база `context`), Redis, gRPC-доступ к incident-service (`9091`), review-service (`9092`) и course-service (`9095`).

```bash
./mvnw spring-boot:run
```

Адреса задаются переменными `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_DATA_REDIS_HOST`, `INCIDENT_SERVICE_GRPC_TARGET`, `REVIEW_SERVICE_GRPC_TARGET`, `COURSE_SERVICE_GRPC_TARGET` (по умолчанию localhost).

Flyway-история сведена в единую `V1`: старую локальную базу `context` нужно пересоздать.

# classifier-service

## Описание

Классификатор происшествий, справочник диспетчерских служб и правила маршрутизации карточек. При первом старте Flyway загружает классификатор из `incident-classifier-v046-24.xlsx`. Порты: HTTP `8087`, gRPC `9093`.

## Локальный запуск

Нужна только PostgreSQL (база `classifier`, по умолчанию `postgres`/`postgres` на `localhost:5432`).

```bash
./mvnw spring-boot:run
```

Переменные при необходимости: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT`, `GRPC_SERVER_PORT`.

# api-gateway

## Описание

Единая точка входа для фронтенда: проверяет JWT и роли, ограничивает частоту запросов (Redis), проксирует запросы (в том числе WebSocket) на остальные сервисы и пишет аудит изменяющих запросов в Kafka. Маршруты — в `src/main/resources/application.yaml`. Порт HTTP: `8080`.

## Локальный запуск

Нужны: Redis, Kafka, публичный ключ JWT (парный к приватному ключу auth-service) и запущенные сервисы, на которые идёт проксирование.

```bash
export JWT_PUBLIC_KEY_PATH=file:../secrets/public.pem
./mvnw spring-boot:run
```

URL сервисов задаются переменными `*_SERVICE_URL` (по умолчанию localhost), а также `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS`. Образец — `.env.example`.

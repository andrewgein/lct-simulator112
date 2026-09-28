# api-gateway

## Описание

Единая точка входа для фронтенда: проверяет JWT и роли, ограничивает частоту запросов (Redis), проксирует запросы (в том числе WebSocket) на остальные сервисы и пишет аудит изменяющих запросов в Kafka. Маршруты — в `src/main/resources/application.yaml`. Порт HTTP: `8080`.

После проверки подписи JWT шлюз проверяет учётную запись через `GET /api/v1/auth/session` в auth-service. Удалённые учётные записи и JWT с устаревшей ролью отклоняются с `401`. При недоступности auth-service защищённые запросы получают `503` (таймаут проверки — 3 секунды). Уже открытые WebSocket-соединения этой проверкой не закрываются.

При развёртывании этой версии сначала обновите auth-service (он должен поддерживать `/api/v1/auth/session`), затем api-gateway и frontend.

## Локальный запуск

Нужны: Redis, Kafka, публичный ключ JWT (парный к приватному ключу auth-service) и запущенные сервисы, на которые идёт проксирование.

```bash
export JWT_PUBLIC_KEY_PATH=file:../secrets/public.pem
./mvnw spring-boot:run
```

URL сервисов задаются переменными `*_SERVICE_URL` (по умолчанию localhost), а также `REDIS_HOST`, `KAFKA_BOOTSTRAP_SERVERS`. Образец — `.env.example`.

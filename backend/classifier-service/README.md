# Classifier Service

Сервис владеет классификатором происшествий, справочником диспетчерских служб и правилами маршрутизации карточек.

## Порты

- HTTP: `8087`
- gRPC: `9093`

## База данных

По умолчанию используется PostgreSQL:

```text
jdbc:postgresql://localhost:5432/classifier
```

Flyway создаёт нормализованную схему и загружает классификатор из `incident-classifier-v046-24.xlsx`.

## REST API

- `GET /api/v1/classifier` — получить классификатор.
- `POST /api/v1/classifier/{classifierCode}/routing` — рассчитать маршрутизацию.
- `GET /api/v1/admin/classifier` — административное представление.

API Gateway направляет эти пути в `classifier-service`.

## gRPC API

Сервис `classifier.ClassifierService`:

- `GetClassifierEntry` — получить запись по стабильному коду;
- `ResolveRouting` — рассчитать решения по службам.

`incident-service` хранит только `classifierCode` и получает данные записи через gRPC. Между базами данных нет внешних
ключей.

## Запуск

```bash
./mvnw spring-boot:run
./mvnw clean test
```

Переменные окружения:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `SERVER_PORT`
- `GRPC_SERVER_PORT`

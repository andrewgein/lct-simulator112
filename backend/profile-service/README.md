# Profile Service

Spring Boot-сервис для создания, получения, обновления и удаления пользовательских профилей. Данные профилей хранятся в
PostgreSQL, а миграции схемы выполняются Flyway.

## Требования

- Java 21
- PostgreSQL
- Docker (необязательно)

## Локальный запуск

Создайте базу данных PostgreSQL с именем `profile`, затем запустите сервис:

```bash
./mvnw spring-boot:run
```

По умолчанию сервис доступен по адресу `http://localhost:8082`. Его можно настроить через переменные окружения:

| Переменная         | Значение по умолчанию                      |
|--------------------|--------------------------------------------|
| `DB_URL`           | `jdbc:postgresql://localhost:5432/profile` |
| `DB_USERNAME`      | `postgres`                                 |
| `DB_PASSWORD`      | `postgres`                                 |
| `SERVER_PORT`      | `8082`                                     |
| `GRPC_SERVER_PORT` | `9092`                                     |

При запуске Flyway автоматически применит миграции базы данных.

## Запуск в Docker

Из каталога `backend`:

```bash
docker build -f profile-service/Dockerfile -t profile-service .
docker run --rm -p 8082:8082 -p 9092:9092 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/profile \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  profile-service
```

## API

Все эндпоинты возвращают JSON и используют базовый путь `/api/v1/profile`.

| Метод    | Эндпоинт    | Описание                                                   |
|----------|-------------|------------------------------------------------------------|
| `GET`    | `/`         | Получить профиль по значению заголовка `X-User-Id`.        |
| `GET`    | `/{userId}` | Получить профиль по идентификатору пользователя.           |
| `GET`    | `/all`      | Получить список всех профилей.                             |
| `POST`   | `/`         | Создать профиль для пользователя из заголовка `X-User-Id`. |
| `PATCH`  | `/`         | Обновить профиль пользователя из заголовка `X-User-Id`.    |
| `PATCH`  | `/{userId}` | Обновить профиль по идентификатору пользователя.           |
| `DELETE` | `/{userId}` | Удалить профиль по идентификатору пользователя.            |

Пример запроса:

```bash
curl -X POST http://localhost:8082/api/v1/profile \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 123e4567-e89b-12d3-a456-426614174000' \
  -d '{"name":"Иван","surname":"Иванов"}'
```

Документация API доступна по адресу `http://localhost:8082/swagger-ui/index.html`.

## Мониторинг

- Проверка состояния: `/actuator/health`
- Метрики Prometheus: `/actuator/prometheus`

## Тесты

```bash
./mvnw test
```

# Incident Service

`incident-service` хранит учебные происшествия, уровни, этапы вызовов и диалапы. Классификатор и маршрутизация принадлежат отдельному `classifier-service`.

Сервис предоставляет REST API для frontend и административной части, а также gRPC API для взаимодействия с другими микросервисами.

## Стек

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- PostgreSQL
- Flyway
- gRPC и Protocol Buffers
- Apache POI
- Maven Wrapper

## Запуск

По умолчанию сервис использует следующие настройки:

| Параметр | Значение |
|---|---|
| HTTP порт | `8086` |
| gRPC порт | `9091` |
| База данных | `jdbc:postgresql://localhost:5432/incident` |
| Пользователь БД | `postgres` |
| Пароль БД | `postgres` |

Настройки подключения можно изменить переменными окружения:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
```

Запуск сервиса:

```bash
./mvnw spring-boot:run
```

Запуск тестов:

```bash
./mvnw clean test
```

При старте Flyway создаёт схему и загружает классификатор из файла:

```text
src/main/resources/classifier/incident-classifier-v046-24.xlsx
```

## Инциденты

### Назначение

Инцидент представляет учебный сценарий для оператора 112.

Структура сценария:

```text
Уровень
└── Инцидент
    └── Этап
        └── Диалап
```

- Уровень объединяет несколько инцидентов и задаёт сложность.
- Инцидент содержит адрес и критерии оценки оператора.
- Этап описывает состояние происшествия и связан с записью классификатора.
- Диалап содержит данные заявителя, известные факты, скрытые факты и контекст для диалога.

### Публичные REST эндпоинты

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/api/v1/incident/levels` | Получить список доступных уровней |

### Административные эндпоинты инцидентов

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/api/v1/admin/incident` | Получить страницу инцидентов. Поддерживает `page`, `size` и `sort` |
| `GET` | `/api/v1/admin/incident/{incidentId}` | Получить инцидент со всеми этапами и диалапами |
| `POST` | `/api/v1/admin/incident` | Создать инцидент |
| `PATCH` | `/api/v1/admin/incident/{incidentId}` | Частично обновить инцидент |
| `DELETE` | `/api/v1/admin/incident/{incidentId}` | Удалить инцидент |

Пример создания инцидента:

```json
{
  "title": "Пожар в жилом доме",
  "address": {
    "city": "Москва",
    "street": "Тверская",
    "house": "1",
    "building": null,
    "apartment": "15",
    "floor": 5
  },
  "criteria": {
    "requiredQuestions": [
      "Есть ли пострадавшие?"
    ],
    "expectedActions": [
      "Уточнить адрес"
    ],
    "criticalMistakes": [
      "Не уточнен адрес"
    ]
  }
}
```

### Административные эндпоинты уровней

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/api/v1/admin/incident/levels` | Получить список уровней |
| `GET` | `/api/v1/admin/incident/levels/{id}` | Получить уровень |
| `POST` | `/api/v1/admin/incident/levels` | Создать уровень |
| `PATCH` | `/api/v1/admin/incident/levels/{id}` | Частично обновить уровень |
| `DELETE` | `/api/v1/admin/incident/levels/{id}` | Удалить уровень |
| `PUT` | `/api/v1/admin/incident/levels/{levelId}/incidents/{incidentId}` | Добавить инцидент в уровень |
| `DELETE` | `/api/v1/admin/incident/levels/{levelId}/incidents/{incidentId}` | Удалить инцидент из уровня |

Пример создания уровня:

```json
{
  "title": "Базовый уровень",
  "difficulty": "EASY"
}
```

Допустимые значения сложности:

```text
EASY
NORMAL
HARD
```

### Административные эндоинты этапов

| Метод | Endpoint | Описание |
|---|---|---|
| `POST` | `/api/v1/admin/incident/incidents/{incidentId}/stages` | Создать этап инцидента |
| `PATCH` | `/api/v1/admin/incident/stages/{id}` | Частично обновить этап |
| `DELETE` | `/api/v1/admin/incident/stages/{id}` | Удалить этап |

Пример создания этапа:

```json
{
  "title": "Первичный вызов",
  "position": 0,
  "classifierCode": "1010101",
  "description": "Заявитель сообщает о возгорании мусора",
  "victim": null
}
```

### Административные эндпоинты диалапов

| Метод | Endpoint | Описание |
|---|---|---|
| `POST` | `/api/v1/admin/incident/stages/{stageId}/dialups` | Создать диалап этапа |
| `PATCH` | `/api/v1/admin/incident/dialups/{id}` | Частично обновить диалап |
| `DELETE` | `/api/v1/admin/incident/dialups/{id}` | Удалить диалап |

### gRPC GetIncidentContext

Метод возвращает уровень вместе с инцидентами, этапами, диалапами и данными классификатора.

```protobuf
rpc GetIncidentContext(GetIncidentContextRequest) returns (LevelContext);
```

Запрос:

```protobuf
message GetIncidentContextRequest {
  string level_id = 1;
}
```

Каждый `StageContext` содержит `ClassifierEntry` с кодом, названием и основными службами.

## Интеграция с классификатором

Этап хранит стабильный `classifierCode`, но не содержит внешнего ключа в другую базу данных. При создании или изменении этапа `incident-service` проверяет код через gRPC `classifier-service`. При формировании REST- и gRPC-представлений сведения классификатора также запрашиваются через gRPC.

Настройки клиента:

```text
CLASSIFIER_GRPC_HOST (по умолчанию localhost)
CLASSIFIER_GRPC_PORT (по умолчанию 9093)
```

REST endpoints `/api/v1/classifier/**` и `/api/v1/admin/classifier/**` обслуживает `classifier-service`; API Gateway сохраняет прежние внешние URL.

## Служебные endpoints

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/actuator/health` | Состояние сервиса |
| `GET` | `/actuator/prometheus` | Метрики Prometheus |
| `GET` | `/v3/api-docs` | OpenAPI описание |
| `GET` | `/swagger-ui.html` | Swagger UI |

Реализация классификатора находится в `backend/classifier-service`.

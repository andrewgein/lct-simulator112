# Incident Service

`incident-service` хранит учебные происшествия, уровни, этапы вызовов и диалапы, а также полный классификатор происшествий и правила маршрутизации карточек по службам.

Сервис предоставляет REST API для frontend и административной части, а также gRPC API для взаимодействия с другими микросервисами.

## Технологии

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

### Публичные REST endpoints

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/api/v1/incident/levels` | Получить список доступных уровней |

### Административные endpoints инцидентов

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

### Административные endpoints уровней

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

### Административные endpoints этапов

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

### Административные endpoints диалапов

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

## Классификатор

### Назначение

Классификатор определяет:

- категории происшествий;
- признаки происшествия;
- итоговый тип происшествия;
- основные службы;
- маршрутные каналы внутри служб;
- условия выбора варианта;
- действие для каждой службы.

Основные сущности:

```text
ClassifierCategoryEntity
ClassifierEntryEntity
DispatchServiceEntity
RoutingVariantEntity
RoutingVariantConditionEntity
RoutingRuleEntity
```

### REST endpoints классификатора

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/api/v1/incident/classifier` | Получить классификатор с категориями, типами и основными службами |
| `POST` | `/api/v1/incident/classifier/{classifierCode}/routing` | Рассчитать маршрутизацию по коду классификатора и фактам карточки |
| `GET` | `/api/v1/admin/incident/classifier` | Получить классификатор для административного интерфейса |

### Расчёт маршрутизации

Пример запроса:

```http
POST /api/v1/incident/classifier/1010101/routing
Content-Type: application/json
```

```json
{
  "facts": {
    "ACCESS_STATUS": "AVAILABLE",
    "OFFENSE_STATUS": "PRESENT",
    "VICTIM_STATUS": "PRESENT",
    "GASIFICATION": "TRUE",
    "THREAT_TO_PEOPLE": "TRUE"
  }
}
```

Пример ответа:

```json
{
  "classifierCode": "1010101",
  "incidentTypeName": "пожар: мусор",
  "facts": {
    "ACCESS_STATUS": "AVAILABLE",
    "VICTIM_STATUS": "PRESENT"
  },
  "decisions": [
    {
      "service": {
        "id": "00000000-0000-0000-0000-000000000000",
        "code": "MCHS",
        "name": "Классификатор МЧС"
      },
      "routingTarget": "Служба 101",
      "matchedVariant": "Служба 101 (признак НД - НЕТ ДОСТУПА не выбран)",
      "resultKind": "SERVICE_TYPE",
      "targetTypeName": "пожар: мусор"
    }
  ]
}
```

Правила группируются по службе и маршрутному каналу. Для каждой группы выбирается одно подходящее правило. Сначала учитывается `priority`, затем позиция колонки в исходном XLSX.

Отсутствующее значение и значение `UNKNOWN` не считаются отрицательным ответом. Например, условие `NOT_EQUALS` не срабатывает, пока фактическое значение неизвестно.

Поддерживаемые операторы условий:

```text
EQUALS
NOT_EQUALS
IN
NOT_IN
EXISTS
NOT_EXISTS
```

Виды результата маршрутизации:

| Значение | Описание |
|---|---|
| `SEND_CARD` | Передать стандартную карточку 112 |
| `SERVICE_TYPE` | Передать карточку с внутренним типом происшествия службы |
| `NO_RESPONSE` | Служба не реагирует |
| `INFORMATION_ONLY` | Только проинформировать службу |
| `RAW` | Использовать ненормализованное значение правила |

### Факты маршрутизации

В текущей версии классификатора используются следующие коды фактов:

```text
ACCESS_STATUS
THREAT_TO_PEOPLE
CASUALTY_STATUS
OFFENSE_STATUS
VICTIM_STATUS
GASIFICATION
MEDICAL_HELP_REQUIRED
EVACUATION_REQUIRED
LARGE_GROUP_OR_OD
TRAFFIC_BLOCKED
LOCATION_KIND
ROAD_USER_KIND
COMMUNICATION_FACILITY
CONSTRUCTION_SITE
LISTED_OBJECT
POLYGON_EVENT
LOCATION
```

Коды и значения передаются строками. Сервис нормализует регистр и пробелы перед расчётом.

### gRPC ResolveRouting

```protobuf
rpc ResolveRouting(ResolveRoutingRequest) returns (RoutingResult);
```

```protobuf
message ResolveRoutingRequest {
  string classifier_code = 1;
  map<string, string> facts = 2;
}
```

`RoutingResult` содержит код и название типа происшествия, нормализованные факты и список решений по службам.

gRPC ошибки:

| Код | Причина |
|---|---|
| `INVALID_ARGUMENT` | Не передан код классификатора или переданы некорректные данные |
| `NOT_FOUND` | Запись классификатора не найдена |
| `INTERNAL` | Внутренняя ошибка расчёта |

## Служебные endpoints

| Метод | Endpoint | Описание |
|---|---|---|
| `GET` | `/actuator/health` | Состояние сервиса |
| `GET` | `/actuator/prometheus` | Метрики Prometheus |
| `GET` | `/v3/api-docs` | OpenAPI описание |
| `GET` | `/swagger-ui.html` | Swagger UI |

## Структура пакетов классификатора

```text
controller/classifier
controller/admin/classifier
dto/request/classifier
dto/view/classifier
exception/classifier
grpc/classifier
mapper/classifier
model/entity/classifier
model/enums/classifier
repository/classifier
service/classifier
```

Код обычных инцидентов остаётся в исходных пакетах. Код классификатора и маршрутизации изолирован в подпакетах `classifier`.

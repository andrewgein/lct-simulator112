# Incident Service

`incident-service` хранит уровни и два независимых типа учебных происшествий:

- `SYSTEM_112` — сценарий оператора системы 112 со звонками заявителей;
- `DDS` — сценарий оператора ДДС с подготовленной карточкой, этапами реагирования, звонками бригад и бинарными
  переходами между этапами.

Классификатор принадлежит `classifier-service`; здесь хранятся только стабильные коды.

## Архитектура

```text
com/simulator112/incident
├── domain
│   ├── common
│   ├── system112
│   └── dds
├── application
│   ├── port/in
│   ├── port/out
│   └── service
└── adapter
    ├── in
    │   ├── rest
    │   └── grpc
    └── out
        ├── persistence
        └── classifier
```

Домен не зависит от Spring, JPA, REST или gRPC. `IncidentApplicationService` реализует входные use-case порты.
PostgreSQL и classifier gRPC client реализуют выходные порты.

## REST API

| Метод  | Endpoint                                          | Описание                        |
|--------|---------------------------------------------------|---------------------------------|
| `POST` | `/api/v1/incidents`                               | Создать происшествие            |
| `PUT`  | `/api/v1/incidents/{incidentId}`                  | Полностью заменить происшествие |
| `GET`  | `/api/v1/incidents/{incidentId}`                  | Получить происшествие           |
| `GET`  | `/api/v1/incidents?targetType=...&difficulty=...` | Найти доступные происшествия (фильтры необязательны) |

`IncidentRequest.targetType` принимает `SYSTEM_112` или `DDS`. Уровни доступны через `/api/v1/levels`; каждый уровень содержит только один тип инцидентов и задаёт режим `SEQUENTIAL` или `PARALLEL`.

## gRPC API

```protobuf
rpc GetIncident(GetIncidentRequest) returns (IncidentContext);
    rpc FindAvailableIncidents(FindAvailableIncidentsRequest)
    returns (FindAvailableIncidentsResponse);
```

## Хранение

Flyway создаёт нормализованные таблицы:

- `incidents`;
- `levels` и упорядоченные связи `level_incidents`;
- `incident_stages` с общими данными этапа;
- `system112_stage_details` и `dds_stage_details` со специализированными данными;
- `call_scenarios` с направлением `INBOUND`/`OUTBOUND` и контрагентом `CALLER`/`BRIGADE`;
- таблицы критериев и фактов звонка;
- `prepared_card_additional_info`.

Для `SYSTEM_112` этапы выполняются линейно и имеют `position`. Для `DDS` этап имеет тип и положительное ограничение
времени, а порядок задаётся через `initialStageId` и переходы `successStageId`/`failureStageId`. Правильное действие или
ожидаемое изменение статуса может завершить этап до истечения времени. Таймаут ведёт по failure-переходу.

Звонки в DDS разрешены только для `CALL_BRIGADE_FOR_STATUS`; такой этап должен содержать исходящий звонок бригаде.
Остальные типы DDS-этапов не должны содержать звонков. Если failure-переход отсутствует, ошибка завершает прохождение со
статусом `FAILED`; отсутствие success-перехода означает успешный финал.

JSONB и межбазовые внешние ключи не используются.

## Запуск

```bash
./mvnw spring-boot:run
./mvnw clean test
```

| Параметр        | Значение по умолчанию                       |
|-----------------|---------------------------------------------|
| HTTP            | `8086`                                      |
| gRPC            | `9091`                                      |
| DB              | `jdbc:postgresql://localhost:5432/incident` |
| classifier gRPC | `localhost:9093`                            |

Переменные окружения: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CLASSIFIER_GRPC_HOST`, `CLASSIFIER_GRPC_PORT`.

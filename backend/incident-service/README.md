# Incident Service

`incident-service` хранит уровни и два независимых типа учебных происшествий:

- `SYSTEM_112` — сценарий оператора системы 112 со звонками заявителей;
- `DDS` — сценарий оператора ДДС с подготовленной карточкой, линейными этапами реагирования и звонками бригад и служб.

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
- `call_scenarios` с направлением `INBOUND`/`OUTBOUND` и контрагентом `CALLER`/`BRIGADE`/`SERVICE`;
- таблицы критериев и фактов звонка;
- `prepared_card_additional_info`.

Для `SYSTEM_112` этапы выполняются линейно и имеют `position`. Для `DDS` этапы идут по порядку в списке:
первый — принятие карточки (30 секунд), последующие — этапы с положительной длительностью. По истечении
времени этап переключается независимо от действий диспетчера. Фактические статусы сценария и внесённые
диспетчером статусы хранятся отдельно и сравниваются при разборе.

На любом этапе DDS допускаются исходящие звонки бригаде или другой службе и входящие звонки бригады.
Пропущенные звонки не останавливают реагирование и учитываются при разборе.

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

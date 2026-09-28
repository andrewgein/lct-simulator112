# Context Service

`context-service` создаёт снимок выбранного уровня и хранит состояние конкретного прохождения.

## Архитектура

```text
com/simulator112/contextmanager
├── domain
│   ├── common
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
        ├── grpc
        └── persistence
```

REST, gRPC и scheduler зависят только от входных портов. Клиенты incident/review и persistence-адаптер реализуют выходные порты. Application-слой работает только с domain/application-моделями: protobuf, REST DTO, JPA-сущности и Spring Data repositories не пересекают границы адаптеров.

Модуль `shared` используется как ограниченный Shared Kernel: в нём допустимы стабильные value objects (`Difficulty`, `EmotionalState`) и protobuf-контракты, но не агрегаты context-service, use cases или бизнес-правила.

## Модель прохождения

- один Context соответствует одному Level;
- Level содержит только `SYSTEM_112` или только `DDS` инциденты;
- `SEQUENTIAL` активирует инциденты по очереди;
- `PARALLEL` перемежает звонки SYSTEM_112 либо запускает независимые DDS-таймеры одновременно.

Для SYSTEM_112 сервис формирует очередь `CallScenario`. Legacy-понятие `Dialup` удалено.

Для DDS сервис хранит этапы по порядку, время начала и deadline. Этапы переключаются только по времени, независимо от действий диспетчера; просроченные этапы обрабатываются фоновым scheduler. Фактический статус сценария и история введённых диспетчером статусов не смешиваются. В последовательном режиме после окончания этапов активируется следующий инцидент.

## REST

- `POST /api/v1/context` — создать прохождение по `levelId`;
- `GET /api/v1/context/{id}/progress` — получить единый прогресс уровня для SYSTEM_112 или DDS;
- `POST /api/v1/context/{id}/dds/incidents/{incidentId}/comments` — сохранить комментарий по этапу DDS;
- `POST /api/v1/context/{id}/dds/incidents/{incidentId}/reaction-status` — внести статус в карточку диспетчера;
- endpoints `/calls/{callId}/cards` и `/cards/{cardId}/revisions` управляют карточками решений;
- карточки поддерживают связь `LINK`/`UNLINK` через `mainCardId`: подчинённая карточка хранит идентификатор главной; связь не меняет жизненный цикл ни одной карточки.

## Хранение

Используется нормализованная PostgreSQL-схема без JSONB. Снимок уровня не имеет внешних ключей в БД incident-service: исходные UUID сохраняются как идентификаторы источника.

Flyway history переписана в единую `V1`; существующую локальную БД context-service перед запуском нужно пересоздать.

## Проверка

```bash
./mvnw clean test
```

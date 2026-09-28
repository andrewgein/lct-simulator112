# lct-simulator112

Учебный симулятор подготовки диспетчеров экстренных служб города по вызовам от системы 112. Проект сделан в рамках Хакатона ЛЦТ 2026

Действующий проект: https://sim-112.ru

## Архитектура
![sim112.png](docs/sim112.png)

## Стек технологий

### Бэкенд
*   **Java 21**
*   **Spring Boot**
*   **gRPC**
*   **Apache Kafka** (асинхронное взаимодействие)
*   **PostgreSQL** + **Flyway** (основное хранилище данных, миграции)
*   **Redis** (rate limit в gateway, состояние context-service)
*   **MinIO** (материалы курсов, записи звонков, бэкапы)
*   **Docker**

### Фронтенд
*   **Astro (SSR)**
*   **Preact**
*   **Web Awesome**
*   **Leaflet**

### Голосовой диалог и LLM (dialog-service)
*   **Python 3.12**
*   **FastAPI**
*   **Vosk** (распознавание речи)
*   **F5-TTS** (синтез речи)
*   **LLM** - qwen3:4b-instruct-2507-q4_K_M
*   **Ollama**: AI-генерация черновиков происшествий в конструкторе (incident-service)

### Тестирование
*   **pytest** (dialog-service)
*   **SpringBootTest**
*   **node:test**

### Инфраструктура
*   **Docker Compose**
*   **GitHub Actions**
*   **Prometheus**, **Grafana**, **Loki**
*   **nginx**

## Архитектура и сервисы

Система состоит из микросервисов, взаимодействующих через API Gateway, gRPC и шину событий Kafka

### Frontend
| Сервис | Порт | Назначение |
|---|---|---|
| [frontend](frontend) | 80 (в докере) / 4321 (dev) | Интерфейс студента, преподавателя и администратора |

### Микросервисы
| Сервис | HTTP | gRPC | Назначение |
|---|---|---|---|
| [api-gateway](backend/api-gateway) | 8080 | — | Единая точка входа, JWT, роли, rate limit |
| [auth-service](backend/auth-service) | 8081 | — | Регистрация, вход, refresh-токены, роли |
| [profile-service](backend/profile-service) | 8082 | 9094 | Профили пользователей |
| [context-service](backend/context-service) | 8084 | 9090 | Состояние прохождения уровня |
| [review-service](backend/review-service) | 8085 | 9092 | Проверка и оценка прохождений |
| [incident-service](backend/incident-service) | 8086 | 9091 | Уровни и происшествия, AI-генерация сценария |
| [classifier-service](backend/classifier-service) | 8087 | 9093 | Классификатор происшествий и маршрутизация служб |
| [notification-service](backend/notification-service) | 8090 | — | Уведомления и письма |
| [course-service](backend/course-service) | 8088 | 9095 | Курсы, учебные группы, сертификаты |
| [admin-service](backend/admin-service) | 8089 | — | Мониторинг, аудит, бэкапы, управление сервисами |
| [dialog-service](backend/dialog-service) | 8005 | — | Голосовой диалог: STT → LLM → TTS (Python) |
| [shared](backend/shared) | — | — | Общие protobuf-контракты и несколько value objects |
| [prod](prod) | — | — | docker-compose, nginx, мониторинг |
### Kafka Topics

*   **user.created** (`auth-service` → `notification-service`): регистрация пользователя
*   **email.verification.requested** (`auth-service` → `notification-service`): подтверждение email
*   **password.reset.requested** (`auth-service` → `notification-service`): сброс пароля
*   **review.comment.created** (`review-service` → `notification-service`): новый комментарий к проверке
*   **certificate.issued** (`course-service` → `notification-service`): выдан сертификат
*   **audit.http.requests** (`api-gateway` → `admin-service`): аудит HTTP-запросов
*   **audit.domain.events** (`auth-service` → `admin-service`): аудит доменных событий

## Хранилища данных

У каждого сервиса своя база в общем PostgreSQL (список баз в [init-db.sql](init-db.sql)), внешних ключей между базами нет:
*   `auth` — пользователи и токены
*   `profile` — профили
*   `incident` — уровни и происшествия
*   `context` — состояние прохождений
*   `review` — оценки и проверки
*   `notification` — уведомления
*   `classifier` — справочник происшествий
*   `course` — курсы, группы, сертификаты
*   `admin` — аудит и настройки

**Redis** используется gateway для rate limit и context-service. <br>
**MinIO**: бакеты `course-materials`, `backups`, `call-recordings`.

## Маршруты
Актуальный список маршрутов: [application.yaml](backend/api-gateway/src/main/resources/application.yaml).

## Локальный запуск
<!-- TODO -->
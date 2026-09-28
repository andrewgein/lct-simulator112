# admin-service

## Описание

Админская обвязка: мониторинг (Prometheus), логи (Loki), аудит событий из Kafka, бэкапы баз в MinIO по расписанию (`pg_dump` + gpg) и управление сервисами через GitHub Actions. Порт HTTP: `8089`. Все пути только для роли `ADMIN`.

## Локальный запуск

Нужны: PostgreSQL (база `admin`), Kafka, MinIO (бакет `backups`), Prometheus, Loki, установленные `mvn`, `pg_dump` и `gpg`.

```bash
mvn spring-boot:run
```

Основные переменные (по умолчанию рассчитаны на localhost): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `PROMETHEUS_BASE_URL`, `LOKI_BASE_URL`, `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `BACKUP_ENCRYPTION_KEY`, `GITHUB_REPOSITORY`, `GITHUB_ACTIONS_TOKEN`.

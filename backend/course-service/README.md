# course-service

## Описание

Курсы, учебные группы, записи и прогресс студентов, сертификаты. Файлы материалов (до 25 МБ) хранятся в MinIO. Принимает результаты оценок от review-service и публикует `certificate.issued`. Порты: HTTP `8088`, gRPC `9095`.

## Локальный запуск

Нужны: PostgreSQL (база `course`), Kafka, MinIO (бакет `course-materials`) и gRPC-доступ к review-service (`9092`), incident-service (`9091`), classifier-service (`9093`).

```bash
./mvnw spring-boot:run
```

Адреса задаются переменными `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `REVIEW_GRPC_HOST/PORT`, `INCIDENT_GRPC_HOST/PORT`, `CLASSIFIER_GRPC_HOST/PORT`, `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` (по умолчанию localhost и `minioadmin`).

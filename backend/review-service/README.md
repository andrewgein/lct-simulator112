# review-service

## Описание

Проверяет завершённые прохождения и считает баллы для операторов 112 и ДДС: автоматическая оценка по рубрикам, семантическая проверка диалога (NLI-модель через MLServer), рекомендации на основе LLM, корректировка балла и комментарии преподавателя. Порты: HTTP `8085`, gRPC `9092`.

## Локальный запуск

Нужны: PostgreSQL (база `review`), Kafka, course-service по gRPC (`9095`), MinIO (бакет `call-recordings`), LLM с OpenAI-совместимым API и MLServer с моделью `rubert-tiny-bilingual-nli` (настройки — `mlserver/rubert-tiny-bilingual-nli/model-settings.json`).

```bash
./mvnw spring-boot:run
```

Адреса задаются переменными `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `COURSE_GRPC_HOST/PORT`, `MLSERVER_URL`, `LLM_BASE_URL`, `LLM_API_KEY`, `LLM_MODEL`, `S3_ENDPOINT`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` (по умолчанию localhost).

Flyway-история сведена в единую `V1`: старую локальную базу `review` нужно пересоздать.

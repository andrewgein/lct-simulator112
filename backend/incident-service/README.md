# incident-service

## Описание

Хранит учебные происшествия двух типов — `SYSTEM_112` (звонки заявителей) и `DDS` (этапы реагирования с таймерами и переходами) — и генерирует их черновики через LLM (Ollama). Данные классификатора берёт у classifier-service по gRPC. Порты: HTTP `8086`, gRPC `9091`.

## Локальный запуск

Нужны: PostgreSQL (база `incident`), classifier-service по gRPC (`9093`) и Ollama (только для генерации черновиков).

```bash
export OLLAMA_CHAT_URL=http://localhost:11434/api/chat
./mvnw spring-boot:run
```

Значение `OLLAMA_CHAT_URL` по умолчанию — адрес из внутренней сети Tailscale, локально его нужно переопределить. Остальные переменные: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CLASSIFIER_GRPC_HOST/PORT`, `OLLAMA_MODEL`.

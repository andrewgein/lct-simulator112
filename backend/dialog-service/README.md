# dialog-service

## Описание

Голосовой пайплайн: речь оператора (Vosk) → текст → ответ LLM → синтез речи заявителя или бригады ДДС (внешний F5-TTS Server на GPU). Принимает WebSocket от браузера, берёт сценарий звонка у context-service по gRPC и сохраняет запись разговора в MinIO. JWT не проверяет — это делает api-gateway. Python 3.12, FastAPI. Порт: `8005`.

## Локальный запуск

Нужны: LLM с OpenAI-совместимым API, F5-TTS Server, MinIO (бакет `call-recordings`), context-service по gRPC (или `CONTEXT_SOURCE=mock` для работы без Java-стека).

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env          # заполнить LLM_API_KEY
uvicorn app.main:app --host 0.0.0.0 --port 8005
```

F5-TTS Server поднимается отдельно на машине с GPU:

```bash
uvicorn f5-tts_server.server:app --host 0.0.0.0 --port 7860
```

Его адрес указывается в `F5_TTS_BASE_URL`. Полный список переменных — в `.env.example`. `CONTEXT_MANAGER_GRPC_URL` должен указывать на gRPC context-service (`127.0.0.1:9090`).

# dialog-service

FastAPI-сервис голосового пайплайна (STT → LLM → TTS) для симулятора звонков операторов системы 112 и операторов ДДС.

## Архитектура

```text
app/
├── domain/model/                         # CallScenario, DialogProgress, transcript
├── application/
│   ├── model/                            # построение LLM-промптов
│   ├── port/inbound/                     # DialogUseCase
│   ├── port/outbound/                    # ContextPort, VoicePipelineFactory
│   └── service/                          # реализация DialogUseCase, transcript builder
├── adapter/
│   ├── config/                           # сборка зависимостей
│   ├── inbound/websocket/                # входной WebSocket adapter
│   └── out/
│       ├── grpc/                         # context-service adapter и mapper
│       ├── mock/                         # локальный context adapter
│       └── processing/                   # цельный STT → LLM → TTS pipeline
└── grpc/                                 # сгенерированные protobuf transport-модели
```

Domain и application не зависят от FastAPI, gRPC или protobuf. WebSocket adapter вызывает
только `DialogUseCase` и `VoicePipelineFactory`, не обращаясь к application service или
outbound adapters напрямую. Преобразование новых `CallScenario` и `DialogProgress`
выполняется только в gRPC adapter. Для заявителя системы 112
и представителя бригады ДДС используются отдельные системные роли LLM.
Vosk распознаёт речь на CPU, LLM вызывается по внешнему API, а синтез речи
выполняет отдельный [F5-TTS Server](https://github.com/ValyrianTech/F5-TTS_server)
через HTTP (`F5_TTS_BASE_URL`).

## Авторизация

`dialog-service` не проверяет JWT. При подключении через gateway проверку доступа
нужно настроить на WebSocket-маршрутах `/api/v1/dialog/**`; доступ к порту
сервиса должен быть ограничен сетью.

## Установка зависимостей

F5-TTS запускается отдельным сервером, поэтому Python-сервису не нужны локальные ML-зависимости
или веса TTS-модели.

```bash
python3 -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

`requirements.txt` содержит только runtime-зависимости. Для перегенерации gRPC-кода
установите инструменты разработки: `pip install -r requirements-dev.txt`.

## Запуск

```bash
cp .env.example .env   # заполнить LLM_API_KEY и остальное
uvicorn app.main:app --host 0.0.0.0 --port 8005
```

Полный список переменных окружения — в `.env.example`. Коротко: `LLM_*` (доступ к LLM),
`CONTEXT_SOURCE`/`CONTEXT_MANAGER_GRPC_URL` (откуда брать контекст диалога — `mock` для локальной
разработки без Java-стека, `grpc` для реального `context-service`), `F5_TTS_*` и `TTS_*` (синтез
речи).

## TTS

Соберите и запустите [F5-TTS Server](https://github.com/ValyrianTech/F5-TTS_server) на машине с
GPU (`uvicorn f5-tts_server.server:app --host 0.0.0.0 --port 7860`) и укажите его адрес в
`F5_TTS_BASE_URL` (по умолчанию в `.env.example` — `http://127.0.0.1:7860`). Dialog-service один раз
за время жизни процесса загружает каждый используемый reference voice через `/upload_audio/` и
для следующих ответов вызывает только `/synthesize_speech/`. Метка голоса содержит хеш аудио,
поэтому изменённый reference автоматически регистрируется как новая версия.

Модульные тесты HTTP-контракта и декодирования WAV не требуют запущенного сервера:

```bash
python -m unittest discover -s tests -v
```

Для проверки реального F5-TTS Server:

```bash
F5_TTS_INTEGRATION_TEST=1 F5_TTS_BASE_URL=http://127.0.0.1:7860 \
  python -m unittest tests.test_tts_model.F5TTSIntegrationTests -v
```

При необходимости сконвертируйте OGG/M4A-референсы в WAV. Скрипт сохраняет исходные файлы и
обновляет `ref_audio` в манифестах:

```bash
python scripts/convert_voice_references_to_wav.py
```

Чтобы сначала просмотреть план без изменений, добавьте `--dry-run`.

Затем можно сгенерировать один и тот же текст всеми комбинациями голоса и эмоции:

```bash
python scripts/generate_voice_emotion_samples.py \
  --text "Проверка синтеза речи."
```

Результаты записываются в `resources/generated_voice_samples/<category>/<voice>/<emotion>.wav`.
Существующие файлы пропускаются; используйте `--overwrite`, чтобы перезаписать их. Для запуска
только отдельных профилей можно повторить `--profile`, например
`--profile man-artyom-calm --profile woman-diana-worried`.

## Наблюдаемость

- `GET /health` — простой liveness-чек.
- `GET /metrics` — Prometheus-метрики (`prometheus-fastapi-instrumentator`).
- Каждое входящее HTTP/WS-подключение логируется с адресом клиента (`ConnectionLoggingMiddleware` в
  `app/main.py`).

## Интеграция

Маршруты `/api/v1/dialog/session` и `/api/v1/dialog/process-call` соответствуют
фронтенду. WebSocket-контракт использует `request_next_call`, `call_ready`, `callId` и
`nextCallAvailable`; gRPC-интеграция — `GetNextCall`, `GetCall`, `StartCall`, `CompleteCall`,
`DisconnectCall`.
Для реальных сценариев укажите `CONTEXT_SOURCE=grpc` и адрес
`CONTEXT_MANAGER_GRPC_URL` из `backend/context-service`; для автономного запуска
можно использовать `CONTEXT_SOURCE=mock`. Dockerfile запускает сервис на порту 8005.

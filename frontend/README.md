# frontend

## Интерфейс

- вход, регистрация, подтверждение почты, профиль;
- прохождение уровня: звонок в браузере (WebSocket на dialog-service), карточка происшествия, карта (Leaflet, 2GIS), подсказки адресов (DaData);
- результаты проверки, комментарии преподавателя, сертификаты, уведомления;
- курсы, учебные группы, назначения, конструктор происшествий с AI-генерацией (страницы `teacher/*`);
- админка: пользователи, мониторинг, логи, аудит, бэкапы, управление сервисами, настройки (`admin/*`);
- справочник классификатора и база знаний.

## Стек

Astro 7 (SSR, адаптер Node), Preact, Web Awesome, Leaflet, TanStack Table, DOMPurify, marked, docx-preview, xlsx, html2pdf.js. Node 22.12+.

```text
src/
├── pages/       # маршруты: admin, teacher, courses, review, profile, api…
├── features/    # логика по доменам: auth, course, dialog, incident, review…
├── components/  # общие компоненты
├── layouts/
├── services/    # ApiClient
├── styles/
└── middleware.js
```

## Конфигурация

| Переменная | Когда | Зачем |
|---|---|---|
| `PUBLIC_API_ENDPOINT` | сборка | адрес gateway, по умолчанию `http://127.0.0.1:8080`. В CI зашивается `http://sim112-stable-gateway:8080` |
| `PUBLIC_DIALOG_ENDPOINT` | сборка | адрес dialog-сервера, если не идёт через gateway |
| `PUBLIC_DADATA_API_KEY` | запуск | ключ DaData, в проде обязателен |
| `DGIS_MAPGL_KEY` | запуск | ключ 2GIS MapGL, необязателен |

## Запуск

```bash
npm ci
npm run dev          # http://localhost:4321
```

В dev-режиме `/api/v1/dialog` проксируется на gateway с подстановкой токена из cookie (см. `astro.config.mjs`). Нужен запущенный api-gateway.

Другие команды: `npm run build`, `npm run preview`.


## Docker

```bash
docker build -f frontend/Dockerfile \
  --build-arg PUBLIC_API_ENDPOINT=http://sim112-stable-gateway:8080 \
  -t frontend frontend
```

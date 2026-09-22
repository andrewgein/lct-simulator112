# Деплой lct-simulator112 в прод

## Архитектура конкретно в нашем случае

```
ethernet -> router VM (nginx + certbot, вне этого репозитория) proxy_pass http://vm-ip -> VM ip:
   * prod/edge/         nginx :80 → маршрутизация по Host: vm-domain.ru(.online) на этот стек;
   * prod/docker-compose.yml, project "sim112-stable" 
        включает dialog-service (в Docker, без GPU - TTS вынесен в HTTP-вызов на кластер компьютеров с GPU)
   * prod/monitoring/   prometheus + grafana (вне router — доступ по Tailscale)
```


## 1. Кластер домашних серверов с GPU — qwentts.cpp

Единственное, что остаётся на этой машине — синтез речи. `dialog-service` в Docker на VM
(раздел 2); сюда VM ходит только за TTS, по Tailscale.

```bash
# NVIDIA-драйверы 
sudo ubuntu-drivers install
nvidia-smi

# Tailscale
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
tailscale status   # запомнить hostname/IP этой машины в тейлнете - это TTS_SERVER_TAILSCALE_HOST

# Ограничить firewall/Tailscale ACL: порт 8888 должен принимать подключения только от VM.
```

Синтез речи (Qwen3-TTS) — отдельный сторонний сервер **qwentts.cpp**, не часть этого репозитория:

```bash
git clone https://github.com/ServeurpersoCom/qwentts.cpp
cd qwentts.cpp
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -DGGML_CUDA=ON
cmake --build build -j "$(nproc)"

hf download Serveurperso/Qwen3-TTS-GGUF \
  qwen-talker-1.7b-base-<quant>.gguf qwen-tokenizer-12hz-<quant>.gguf

./build/tts-server \
  --model "$HOME/.cache/huggingface/hub/models--Serveurperso--Qwen3-TTS-GGUF/snapshots/<hash>/qwen-talker-1.7b-base-<quant>.gguf" \
  --codec "$HOME/.cache/huggingface/hub/models--Serveurperso--Qwen3-TTS-GGUF/snapshots/<hash>/qwen-tokenizer-12hz-<quant>.gguf" \
  --alias qwen3-tts-base --port 8888
```


## 2. VM ip (Alpine)

Если на этой VM уже развёрнут `simulator112` — внешние сети (`prod-edge`, `prod-monitoring`)
уже созданы, `docker network create` заново не нужен.

```bash
# Клонировать репозиторий
git clone -b main https://github.com/andrewgein/lct-simulator112.git
cd lct-simulator112

# Секреты - JWT keypair 
mkdir -p secrets
openssl genrsa -out secrets/private.pem 2048
openssl rsa -in secrets/private.pem -pubout -out secrets/public.pem

# env-файл 
cp prod/.env.example prod/.env
# заполнить DB_PASSWORD/MAIL_*/LLM_*/IMAGE_TAG, TTS_SERVER_TAILSCALE_HOST=<tailscale-IP
# домашнего ПК>

# Если monitoring не настроен:
cp prod/monitoring/.env.example prod/monitoring/.env
```

### Первый запуск нового стека

```bash
# edge-nginx 
cp prod/edge/nginx.conf.template prod/edge/nginx.conf
docker compose -f prod/edge/docker-compose.yml up -d

# monitoring 
cd prod/monitoring
set -a && . ./.env && set +a
envsubst < prometheus.yml.template > prometheus.yml
cd ../..
docker compose -f prod/monitoring/docker-compose.yml up -d

# app-стек 
docker compose -p sim112-stable --env-file prod/.env -f prod/docker-compose.yml up -d
```

`prod/monitoring/prometheus.yml` и `prod/edge/nginx.conf` — сгенерированные файлы (в
.gitignore), перерендеривать нужно только если поменяется сам шаблон.

---

## 3. Router VM

Домены (`vm-domain.ru`, `www.vm-domain.ru`, `vm-domain-112.ru`, `vm-domain.online`,
`www.vm-domain.online`, `dev.vm-domain.online`, `status.vm-domain.online`), DNS и TLS-сертификаты нужно настроить именно
в router VM

---

## 4. Проверка

```bash
curl -I https://vm-domain.ru
curl -I https://vm-domain.online
curl -I https://vm-domain.ru        
curl http://<tailscale-ip-VM>:3001    # Grafana (admin / пароль из monitoring/.env)
curl http://<tailscale-host-ПК>:8888/v1/audio/voices   # qwentts.cpp с VM

docker logs -f sim112-stable-dialog
docker exec sim112-stable-dialog curl -s http://localhost:8005/health

curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Connection: Upgrade" -H "Upgrade: websocket" \
  -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" -H "Sec-WebSocket-Version: 13" \
  --cookie "accessToken=<TOKEN>" \
  "https://sim-112.ru/api/v1/dialog/session?contextId=test"
```

В Grafana (папка `lct-simulator112`) — дашборд "JVM services" (up/HTTP/heap/GC) и
"Infra & dialog-service" (хост/контейнеры VM + доступность dialog-service).

---

## 5. CD

### Секреты для раннера

```bash
mkdir -p ~/lct-simulator112-secrets/secrets ~/lct-simulator112-secrets/prod
cp ~/lct-simulator112/secrets/private.pem ~/lct-simulator112/secrets/public.pem \
  ~/lct-simulator112-secrets/secrets/
cp ~/lct-simulator112/prod/.env ~/lct-simulator112-secrets/prod/
```

### Self-hosted GitHub Actions runner

```bash
sudo ./svc.sh install
sudo ./svc.sh start
```

# TODO переписать deploy.md

#!/usr/bin/env bash

set -euo pipefail

INCIDENT_IMPORT_URL="${INCIDENT_IMPORT_URL:-http://localhost:8086/api/v1/incidents/import}"
CREATE_LEVELS="${CREATE_LEVELS:-true}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"

usage() {
  cat >&2 <<'EOF'
Использование: load-incidents.sh <файл.json> [<файл.json> ...]

Каждый файл содержит JSON-массив инцидентов в формате POST /api/v1/incidents.

Переменные окружения:
  INCIDENT_IMPORT_URL  адрес эндпоинта импорта (по умолчанию http://localhost:8086/api/v1/incidents/import;
                       через шлюз: https://<host>/api/v1/incidents/import)
  ACCESS_TOKEN         JWT администратора, обязателен при загрузке через шлюз
  CREATE_LEVELS        true|false — создать уровни по типу и сложности (по умолчанию true)
EOF
  exit 1
}

for command in jq curl; do
  command -v "$command" >/dev/null || {
    echo "Ошибка: требуется ${command}" >&2
    exit 1
  }
done

[[ $# -gt 0 ]] || usage

for file in "$@"; do
  [[ -f "$file" ]] || {
    echo "Ошибка: файл не найден: ${file}" >&2
    exit 1
  }
  jq -e 'type == "array" and length > 0' "$file" >/dev/null || {
    echo "Ошибка: ${file} должен содержать непустой JSON-массив инцидентов" >&2
    exit 1
  }
done

incidents="$(jq -s '
  def codes: if has("classifierCode") then .classifierCodes = [.classifierCode] | del(.classifierCode) else . end;
  add | map(
    .stages |= map(codes)
    | if .preparedCardTemplate then .preparedCardTemplate |= codes else . end
  )
' "$@")"

jq -e '
  all(.[];
    (.title | type == "string" and length > 0) and
    (.targetType == "SYSTEM_112" or .targetType == "DDS") and
    (.difficulty == "EASY" or .difficulty == "NORMAL" or .difficulty == "HARD") and
    (.stages | type == "array" and length > 0) and
    (if .targetType == "SYSTEM_112"
      then all(.stages[]; .classifierCodes | type == "array" and length > 0)
      else (.preparedCardTemplate.classifierCodes | type == "array" and length > 0)
    end))
' <<<"$incidents" >/dev/null || {
  echo "Ошибка: у каждого инцидента должны быть title, targetType, difficulty, этапы и коды классификатора" >&2
  exit 1
}

jq -r '.[] | "  \(.targetType) \(.difficulty)  \(.title)"' <<<"$incidents"
echo "Загружается инцидентов: $(jq 'length' <<<"$incidents"), создание уровней: ${CREATE_LEVELS}"

request="$(jq -c --argjson createLevels "$CREATE_LEVELS" '{incidents: ., createLevels: $createLevels}' <<<"$incidents")"

headers=(--header 'Content-Type: application/json')
[[ -n "$ACCESS_TOKEN" ]] && headers+=(--header "Authorization: Bearer ${ACCESS_TOKEN}")

response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT

status="$(curl --silent --show-error \
  --request POST \
  --url "$INCIDENT_IMPORT_URL" \
  "${headers[@]}" \
  --data-binary @- \
  --output "$response_file" \
  --write-out '%{http_code}' <<<"$request")"

if [[ "$status" != "201" ]]; then
  echo "Ошибка загрузки (HTTP ${status}):" >&2
  jq -r '.message // .' "$response_file" 2>/dev/null >&2 || cat "$response_file" >&2
  exit 1
fi

jq -r '
  "Загружено инцидентов: \(.incidents | length)",
  (.incidents[] | "  \(.id)  \(.title)"),
  (if (.levels | length) > 0 then "Создано уровней: \(.levels | length)" else empty end),
  (.levels[] | "  \(.id)  \(.title) — инцидентов: \(.incidentIds | length)")
' "$response_file"

#!/usr/bin/env bash

set -euo pipefail

INCIDENT_IMPORT_URL="${INCIDENT_IMPORT_URL:-http://localhost:8086/api/v1/incidents/import}"
CREATE_LEVELS="${CREATE_LEVELS:-true}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"

for command in jq curl; do
  command -v "$command" >/dev/null || {
    echo "Ошибка: требуется ${command}" >&2
    exit 1
  }
done

headers=()
[[ -n "$ACCESS_TOKEN" ]] && headers+=(--header "Authorization: Bearer ${ACCESS_TOKEN}")

response_file="$(mktemp)"
trap 'rm -f "$response_file"' EXIT

echo "Загрузка инцидентов из resources/incidents через ${INCIDENT_IMPORT_URL} (создание уровней: ${CREATE_LEVELS})"

status="$(curl --silent --show-error \
  --request POST \
  --url "${INCIDENT_IMPORT_URL}?createLevels=${CREATE_LEVELS}" \
  ${headers[@]+"${headers[@]}"} \
  --output "$response_file" \
  --write-out '%{http_code}')"

if [[ "$status" != "201" ]]; then
  echo "Ошибка загрузки (HTTP ${status}):" >&2
  jq -r '.message // .' "$response_file" 2>/dev/null >&2 || cat "$response_file" >&2
  exit 1
fi

jq -r '
  "Загружено инцидентов: \(.incidents | length)",
  (.incidents[] | "  \(.targetType) \(.difficulty)  \(.title)"),
  (if (.levels | length) > 0 then "Создано уровней: \(.levels | length)" else empty end),
  (.levels[] | "  \(.id)  \(.title) — инцидентов: \(.incidentIds | length)")
' "$response_file"

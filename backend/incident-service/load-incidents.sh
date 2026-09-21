#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
API_URL="${API_URL:-http://localhost:8086/api/v1}"
ACCESS_TOKEN="${ACCESS_TOKEN:-}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-incident}"
INCIDENTS_DIR="${INCIDENTS_DIR:-${SCRIPT_DIR}/src/main/resources/incidents}"
RESET_EXISTING="${RESET_EXISTING:-true}"

for command in jq curl; do
  command -v "$command" >/dev/null || {
    echo "Ошибка: требуется ${command}" >&2
    exit 1
  }
done

auth_header=()
[[ -n "$ACCESS_TOKEN" ]] && auth_header=(--header "Authorization: Bearer ${ACCESS_TOKEN}")

post_json() {
  local url="$1"
  local body="$2"

  curl --fail-with-body --silent --show-error \
    --request POST \
    --url "$url" \
    --header 'Content-Type: application/json' \
    ${auth_header[@]+"${auth_header[@]}"} \
    --data-binary "$body"
}

shopt -s nullglob
files=("$INCIDENTS_DIR"/*.json)
shopt -u nullglob
if [[ ${#files[@]} -eq 0 ]]; then
  echo "Ошибка: в ${INCIDENTS_DIR} нет файлов *.json" >&2
  exit 1
fi

incidents="$(jq -s 'add' "${files[@]}")"

jq -e '
  type == "array" and length > 0 and
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
  echo "Ошибка: у каждого инцидента должны быть title, targetType, difficulty, этапы и classifierCodes" >&2
  exit 1
}

if [[ "$RESET_EXISTING" == "true" ]]; then
  command -v psql >/dev/null || {
    echo "Ошибка: для RESET_EXISTING=true требуется psql" >&2
    exit 1
  }
  echo "Удаляются существующие уровни и инциденты..."
  psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 --quiet \
    --command 'DELETE FROM levels; DELETE FROM incidents;' >/dev/null
fi

incident_total="$(jq 'length' <<<"$incidents")"
created='[]'
for ((index = 0; index < incident_total; index++)); do
  incident="$(jq -c ".[$index]" <<<"$incidents")"
  title="$(jq -r '.title' <<<"$incident")"
  echo "[$((index + 1))/${incident_total}] $(jq -r '"\(.targetType) \(.difficulty)"' <<<"$incident")  ${title}"
  response="$(post_json "${API_URL}/incidents" "$incident")"
  created="$(jq -c --argjson incident "$incident" --arg id "$(jq -er '.id' <<<"$response")" \
    '. + [{id: $id, targetType: $incident.targetType, difficulty: $incident.difficulty}]' <<<"$created")"
done

level_total=0
while IFS= read -r level; do
  title="$(jq -r '.title' <<<"$level")"
  post_json "${API_URL}/levels" "$level" >/dev/null
  echo "Уровень: ${title} — инцидентов: $(jq '.incidentIds | length' <<<"$level")"
  ((level_total += 1))
done < <(jq -c '
  group_by([.targetType, .difficulty])
  | sort_by(.[0].targetType, (.[0].difficulty | {"EASY": 0, "NORMAL": 1, "HARD": 2}[.]))
  | .[]
  | {
      title: ((if .[0].targetType == "SYSTEM_112" then "Система 112" else "ДДС" end)
        + " — " + ({"EASY": "лёгкий", "NORMAL": "средний", "HARD": "сложный"}[.[0].difficulty])),
      targetType: .[0].targetType,
      difficulty: .[0].difficulty,
      executionMode: "SEQUENTIAL",
      incidentIds: map(.id)
    }
' <<<"$created")

echo "Готово: загружено инцидентов ${incident_total}, создано уровней ${level_total}."

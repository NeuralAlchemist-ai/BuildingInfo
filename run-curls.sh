#!/usr/bin/env bash
BASE_URL="http://localhost:8080/api/building"
JSON_FILE="sample-building.json"

if [ ! -f "$JSON_FILE" ]; then
  echo "Missing $JSON_FILE in current directory"
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required but was not found in PATH" >&2
  exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required but was not found in PATH" >&2
  exit 1
fi

FAILED=0

run_request() {
  local path="$1"
  local response_file
  response_file="$(mktemp)"

  echo "POST $BASE_URL/$path"
  if ! curl -sS -f -X POST -H "Content-Type: application/json" "$BASE_URL/$path" -d @"${JSON_FILE}" >"$response_file"; then
    echo "Request failed for $BASE_URL/$path" >&2
    FAILED=1
  elif ! jq . "$response_file"; then
    echo "Invalid JSON response for $BASE_URL/$path" >&2
    FAILED=1
  fi

  rm -f "$response_file"
  echo
}

run_request "area"
run_request "cube"
run_request "light"
run_request "heating"
run_request "heating/exceeded?threshold=15"
run_request "level/10/area"
run_request "room/101/light"

exit "$FAILED"

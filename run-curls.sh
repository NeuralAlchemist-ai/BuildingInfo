#!/usr/bin/env bash
BASE_URL="http://localhost:8080/api/building"
JSON_FILE="sample-building.json"

if [ ! -f "$JSON_FILE" ]; then
  echo "Missing $JSON_FILE in current directory"
  exit 1
fi

echo "POST $BASE_URL/area"
curl -s -X POST -H "Content-Type: application/json" "$BASE_URL/area" -d @${JSON_FILE} | jq || true

echo "POST $BASE_URL/cube"
curl -s -X POST -H "Content-Type: application/json" "$BASE_URL/cube" -d @${JSON_FILE} | jq || true

echo "POST $BASE_URL/light"
curl -s -X POST -H "Content-Type: application/json" "$BASE_URL/light" -d @${JSON_FILE} | jq || true

echo "POST $BASE_URL/heating"
curl -s -X POST -H "Content-Type: application/json" "$BASE_URL/heating" -d @${JSON_FILE} | jq || true

echo "POST $BASE_URL/heating/exceeded?threshold=15"
curl -s -X POST -H "Content-Type: application/json" "$BASE_URL/heating/exceeded?threshold=15" -d @${JSON_FILE} | jq || true

echo "POST $BASE_URL/level/10/area"
curl -s -X POST -H "Content-Type: application/json" "$BASE_URL/level/10/area" -d @${JSON_FILE} | jq || true

echo "POST $BASE_URL/room/101/light"
curl -s -X POST -H "Content-Type: application/json" "$BASE_URL/room/101/light" -d @${JSON_FILE} | jq || true

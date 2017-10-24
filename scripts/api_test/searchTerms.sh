#!/usr/bin/env bash

QUERY=${1:-"term2"}
PORT=${2:-8888}
curl -v -H "Content-Type: application/json" -X GET http://localhost:${PORT}/term/term -d "{
    \"term\": \"${QUERY}\"
}"

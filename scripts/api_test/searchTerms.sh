#!/usr/bin/env bash

QUERY=${1:-"term2"}
PORT=${2:-8888}
INDEX_NAME=${3:-index_0}
curl -v -H "Content-Type: application/json" -X GET http://localhost:${PORT}/${INDEX_NAME}/term/term -d "{
    \"term\": \"${QUERY}\"
}"

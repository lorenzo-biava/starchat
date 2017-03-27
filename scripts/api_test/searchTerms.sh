#!/usr/bin/env bash

QUERY=${1:-"term2"}
curl -v -H "Content-Type: application/json" -X GET http://localhost:8888/term/term -d "{
    \"term\": \"${QUERY}\"
}"

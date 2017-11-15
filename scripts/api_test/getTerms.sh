#!/usr/bin/env bash

QUERY=${1:-"\"term\""}
PORT=${2:-8888}
INDEX_NAME=${3:-index_0}
curl -v -H "Content-Type: application/json" -X POST http://localhost:${PORT}/${INDEX_NAME}/term/get -d "{
	\"ids\": [${QUERY}]
}"


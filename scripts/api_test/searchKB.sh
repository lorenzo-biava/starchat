#!/usr/bin/env bash

QUERY=${1:-"how are you?"}
PORT=${2:-8888}
INDEX_NAME=${3:-index_0}
curl -v -H "Content-Type: application/json" -X POST http://localhost:${PORT}/${INDEX_NAME}/knowledgebase_search -d "{
	\"question\": \"${QUERY}\",
	\"doctype\": \"normal\",
	\"min_score\": 0.0
}" 


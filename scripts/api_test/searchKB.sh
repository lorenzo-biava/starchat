#!/usr/bin/env bash

QUERY=${1:-"how are you?"}
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/knowledgebase_search -d "{
	\"question\": \"${QUERY}\",
	\"doctype\": \"normal\",
	\"min_score\": 0.0
}" 


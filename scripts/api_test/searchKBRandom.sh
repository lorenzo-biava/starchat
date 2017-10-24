#!/usr/bin/env bash

QUERY=${1:-"how are you?"}
PORT=${2:-8888}
curl -v -H "Content-Type: application/json" -X POST http://localhost:${PORT}/knowledgebase_search -d "{
	\"doctype\": \"normal\",
        \"random\": true, 
	\"size\": 1
}" 


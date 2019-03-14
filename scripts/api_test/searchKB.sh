#!/usr/bin/env bash

QUERY=${1:-"how are you?"}
PORT=${2:-8888}
INDEX_NAME=${3:-index_getjenny_english_0}
ROUTE=${4:-knowledgebase}

	#\"question\": \"${QUERY}\",
	#\"conversation\" : \"9993\",
	#\"sortByConvIdIdx\": true,
curl -v -H "Authorization: Basic $(echo -n 'test_user:p4ssw0rd' | base64)" \
  -H "Content-Type: application/json" -X POST http://localhost:${PORT}/${INDEX_NAME}/${ROUTE}/search -d "{
	\"sortByConvIdIdx\": true,
	\"size\": 100,
	\"doctype\": \"normal\",
	\"minScore\": 0.0
}"


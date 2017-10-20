#!/usr/bin/env bash

QUERY=${1:-"\"term\""}
PORT=${2:-8888}
curl -v -H "Content-Type: application/json" -X POST http://localhost:${PORT}/term/get -d "{
	\"ids\": [${QUERY}]
}"


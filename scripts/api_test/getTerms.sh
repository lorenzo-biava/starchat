#!/usr/bin/env bash

QUERY=${1:-"\"term\""}
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/term/get -d "{
	\"ids\": [${QUERY}]
}"


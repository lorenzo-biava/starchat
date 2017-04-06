#!/usr/bin/env bash

ANALYZER=${1:-"stop"}
QUERY=${2:-"good morning, may I ask you a question?"}
curl -v -H "Content-Type: application/json" -X POST "http://localhost:8888/tokenizers" -d "
{
	\"text\": \"${QUERY}\",
	\"tokenizer\": \"${ANALYZER}\"
}
"


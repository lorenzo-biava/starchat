#!/usr/bin/env bash

ANALYZER=${1:-"stop"}
QUERY=${2:-"good morning, may I ask you a question?"}
PORT=${3:-8888}
curl -v -H "Content-Type: application/json" -X POST "http://localhost:${PORT}/tokenizers" -d "
{
	\"text\": \"${QUERY}\",
	\"tokenizer\": \"${ANALYZER}\"
}
"


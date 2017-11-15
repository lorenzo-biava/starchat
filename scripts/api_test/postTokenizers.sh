#!/usr/bin/env bash

ANALYZER=${1:-"stop"}
QUERY=${2:-"good morning, may I ask you a question?"}
PORT=${3:-8888}
INDEX_NAME=${4:-index_0}
curl -v -H "Content-Type: application/json" -X POST "http://localhost:${PORT}/${INDEX_NAME}/tokenizers" -d "
{
	\"text\": \"${QUERY}\",
	\"tokenizer\": \"${ANALYZER}\"
}
"


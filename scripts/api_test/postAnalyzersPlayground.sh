#!/usr/bin/env bash

ANALYZER=${1:-"keyword(\\\"test\\\")"}
QUERY=${2:-"this is a test"}
DATA=${3:-"{\"item_list\": [], \"extracted_variables\":{}}"}
PORT=${4:-8888}
curl -H "Content-Type: application/json" -X POST "http://localhost:${PORT}/analyzers_playground" -d "
{
	\"analyzer\": \"${ANALYZER}\",
	\"query\": \"${QUERY}\",
	\"data\": ${DATA}
}
"


#!/usr/bin/env bash

ANALYZER=${1:-"keyword(\\\"test\\\")"}
QUERY=${2:-"this is a test"}
DATA=${3:-"{\"item_list\": [], \"extracted_variables\":{}}"}
curl -v -H "Content-Type: application/json" -X POST "http://localhost:8888/analyzers_playground" -d "
{
	\"analyzer\": \"${ANALYZER}\",
	\"query\": \"${QUERY}\",
	\"data\": ${DATA}
}
"


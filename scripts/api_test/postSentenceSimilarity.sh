#!/usr/bin/env bash

QUERY1=${1:-"this is a test"}
QUERY2=${2:-"I'm trying this function"}
PORT=${3:-8888}
curl -s -H "Content-Type: application/json" -X POST "http://localhost:${PORT}/analyzers_playground" -d"
{

	\"analyzer\": \"conjunction(similar(\\\"${QUERY2}\\\"), similarCosEmd(\\\"${QUERY2}\\\"))\", 
	\"query\": \"${QUERY1}\"
}
"



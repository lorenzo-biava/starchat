#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_0}
curl -v -H "Content-Type: application/json" -X POST http://localhost:${PORT}/${INDEX_NAME}/knowledgebase_search -d '{
	"question_scored_terms": "installing"
}' 


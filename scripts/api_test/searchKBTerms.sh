#!/usr/bin/env bash

PORT=${1:-8888}
curl -v -H "Content-Type: application/json" -X POST http://localhost:${PORT}/knowledgebase_search -d '{
	"question_scored_terms": "installing"
}' 


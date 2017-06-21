#!/usr/bin/env bash

curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/knowledgebase_search -d '{
	"question_scored_terms": "installing"
}' 


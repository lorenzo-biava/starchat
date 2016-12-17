#!/usr/bin/env bash

curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/knowledgebase_search -d '{
	"question": "how are you?",
	"verified": true,
	"doctype": "normal",
	"min_score": 0.1
}' 


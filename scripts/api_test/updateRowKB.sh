#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_0}
curl -v -H "Content-Type: application/json" -X PUT http://localhost:${PORT}/${INDEX_NAME}/knowledgebase/0 -d '{
	"conversation": "id:1001",
	"question": "thank you",
	"answer": "you are welcome!",
	"verified": true,
	"topics": "t1 t2",
	"doctype": "normal",
	"state": "",
	"status": 0
}' 


#!/usr/bin/env bash

PORT=${1:-8888}
curl -v -H "Content-Type: application/json" -X PUT http://localhost:${PORT}/knowledgebase/0 -d '{
	"conversation": "id:1001",
	"question": "thank you",
	"answer": "you are welcome!",
	"verified": true,
	"topics": "t1 t2",
	"doctype": "normal",
	"state": "",
	"status": 0
}' 


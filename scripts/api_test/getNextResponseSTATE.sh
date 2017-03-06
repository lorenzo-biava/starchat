#!/usr/bin/env bash

curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/get_next_response -d '{
	"conversation_id": "1234",
	"user_input": { "text": "" },
	"values": {
		"return_value": "further_details_access_question",
		"data": {}
	}
}'

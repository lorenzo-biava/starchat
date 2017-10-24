#!/usr/bin/env bash

PORT=${1:-8888}
curl -v -H "Content-Type: application/json" -X POST http://localhost:${PORT}/get_next_response -d '{
	"conversation_id": "1234",
	"user_input": { "text": "" },
	"values": {
		"return_value": "further_details_access_question",
		"data": {}
	}
}'

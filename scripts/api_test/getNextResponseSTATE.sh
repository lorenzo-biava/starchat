#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_english_0}
curl -v -H "Authorization: Basic `echo -n 'test_user:p4ssw0rd' | base64`" \
  -H "Content-Type: application/json" -X POST http://localhost:${PORT}/${INDEX_NAME}/get_next_response -d '{
	"conversation_id": "1234",
	"user_input": { "text": "" },
	"values": {
		"return_value": "further_details_access_question",
		"data": {}
	}
}'

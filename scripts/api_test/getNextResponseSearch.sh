#!/usr/bin/env bash

QUERY=${1:-"cannot access account"}
PORT=${2:-8888}
INDEX_NAME=${3:-index_0}
curl -v -H "Authorization: Basic `echo -n 'test_user:p4ssw0rd' | base64`" \
 -H "Content-Type: application/json" -X POST http://localhost:${PORT}/${INDEX_NAME}/get_next_response -d "{
	\"conversation_id\": \"1234\",
	\"user_input\": { \"text\": \"${QUERY}\" },
	\"values\": {
		\"return_value\": \"\",
		\"data\": {\"varname1\": \"value1\", \"varname2\": \"value2\"}
	},
	\"threshold\": 0.0,
	\"max_results\": 4
}"

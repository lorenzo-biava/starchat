#!/usr/bin/env bash

QUERY=${1:-"good morning, may I ask you a question?"}
curl -v -H "Content-Type: application/json" -X POST "http://localhost:8888/language_guesser" -d "
{
	\"input_text\": \"${QUERY}\"
}
"


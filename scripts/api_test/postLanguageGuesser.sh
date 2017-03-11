#!/usr/bin/env bash

QUERY=${1:-"ciao"}
curl -v -H "Content-Type: application/json" -X POST "http://localhost:8888/language_guesser" -d "
{
	\"input_text\": \"${QUERY}\"
}
"


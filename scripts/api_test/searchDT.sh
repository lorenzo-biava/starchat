#!/usr/bin/env bash

Q="${1:-'cannot access my account'}"
S="${2:-1.0}"
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/decisiontable_search -d "{
	\"queries\": \"${Q}\",
	\"min_score\": ${S}
}"

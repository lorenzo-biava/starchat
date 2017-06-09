#!/usr/bin/env bash

Q="${1:-'cannot access my account'}"
S="${2:-2.0}"
B="${3:-100.0}"
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/decisiontable_search -d "{
	\"queries\": \"${Q}\",
	\"min_score\": ${S},
	\"boost_exact_match_factor\": ${B},
	\"from\": 0,	
	\"size\": 10
}"

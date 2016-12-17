#!/usr/bin/env bash

curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/decisiontable_search -d '{
	"queries": "cannot access my account",
	"min_score": 0.1
}'

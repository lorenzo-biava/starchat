#!/usr/bin/env bash

curl -v -H "Content-Type: application/json" -X DELETE http://localhost:8888/term -d '{
	"ids": ["मराठी", "term2"]
}'


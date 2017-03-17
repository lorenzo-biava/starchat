#!/usr/bin/env bash

curl -v -H "Content-Type: application/json" -X GET http://localhost:8888/term/term -d '{
    "term": "मराठी"
}'

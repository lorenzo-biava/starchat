#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_getjenny_english_0}
curl -v -H "Authorization: Basic $(echo -n 'test_user:p4ssw0rd' | base64)" \
  -H "Content-Type: application/json" -X DELETE http://localhost:${PORT}/${INDEX_NAME}/knowledgebase -d '{"ids": [0]}'

curl -v -H "Authorization: Basic $(echo -n 'test_user:p4ssw0rd' | base64)" \
  -H "Content-Type: application/json" -X DELETE http://localhost:${PORT}/${INDEX_NAME}/knowledgebase -d '{"ids": [1]}'


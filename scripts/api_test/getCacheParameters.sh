#!/usr/bin/env bash

QUERY=${1:-"\"term2\""}
INDEX_NAME=${2:-index_english_0}
PORT=${3:-8888}
ROUTE=${4:-knowledgebase}
curl -v -H "Authorization: Basic $(echo -n 'admin:adminp4ssw0rd' | base64)" \
  -H "Content-Type: application/json" -X GET http://localhost:${PORT}/${INDEX_NAME}/cache/${ROUTE} 


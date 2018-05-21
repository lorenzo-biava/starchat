#!/usr/bin/env bash

TERM=${1:-"hello"}
FIELD=${2:-"question"}
PORT=${2:-8888}
INDEX_NAME=${3:-index_english_0}
curl -v -H "Authorization: Basic $(echo -n 'test_user:p4ssw0rd' | base64)" \
  -H "Content-Type: application/json" -X GET "http://localhost:${PORT}/${INDEX_NAME}/term_count/knowledgebase?field=${FIELD}&term=${TERM}"


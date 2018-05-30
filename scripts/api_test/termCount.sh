#!/usr/bin/env bash

TERM=${1:-"hello"}
ROUTE=${2:-knowledgebase}
FIELD=${3:-"question"}
PORT=${4:-8888}
INDEX_NAME=${5:-index_english_0}
curl -v -H "Authorization: Basic $(echo -n 'test_user:p4ssw0rd' | base64)" \
  -H "Content-Type: application/json" -X GET "http://localhost:${PORT}/${INDEX_NAME}/term_count/${ROUTE}?field=${FIELD}&term=${TERM}"


#!/usr/bin/env bash

QUERY=${1:-"this is a tes for splellchecker"}
PORT=${2:-8888}
INDEX_NAME=${3:-index_english_0}
curl -v -H "Authorization: Basic $(echo -n 'test_user:p4ssw0rd' | base64)" \
  -H "Content-Type: application/json" -X POST http://localhost:${PORT}/${INDEX_NAME}/spellcheck/terms -d "{
  \"text\": \"${QUERY}\",
  \"prefix_length\": 3,
  \"min_doc_freq\": 1
}"

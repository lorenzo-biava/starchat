#!/usr/bin/env bash

QUERY=${1:-"this is a tes for splellchecker"}
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/spellcheck/terms -d "{
  \"text\": \"${QUERY}\",
  \"prefix_length\": 3,
  \"min_doc_freq\": 1
}"

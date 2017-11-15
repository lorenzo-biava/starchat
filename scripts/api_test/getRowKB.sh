#!/usr/bin/env bash

ID=${1:-0}
PORT=${2:-8888}
INDEX_NAME=${3:-index_0}
# retrieve one or more entries with given ids; ids can be specified multiple times
curl -v -H "Content-Type: application/json" "http://localhost:${PORT}/${INDEX_NAME}/knowledgebase?ids=${ID}"


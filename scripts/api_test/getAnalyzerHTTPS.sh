#!/usr/bin/env bash

PORT=${1:-8443}
INDEX_NAME=${2:-index_0}
curl --insecure -v -H "Content-Type: application/json" -X GET "https://localhost:${PORT}/${INDEX_NAME}/decisiontable_analyzer"


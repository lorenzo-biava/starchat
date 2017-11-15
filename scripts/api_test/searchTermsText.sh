#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_0}
curl -v -H "Content-Type: application/json" -X GET http://localhost:${PORT}/${INDEX_NAME}/term/text -d 'term2 मराठी'

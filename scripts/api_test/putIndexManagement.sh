#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_0}
LANGUAGE=${3:-english}
curl -v -H "Content-Type: application/json" -X PUT "http://localhost:${PORT}/${INDEX_NAME}/${LANGUAGE}/index_management"


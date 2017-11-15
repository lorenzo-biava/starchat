#!/usr/bin/env bash

LANG=${1:-en} 
PORT=${2:-8888}
INDEX_NAME=${3:-index_0}
curl -v -H "Content-Type: application/json" -X GET "http://localhost:${PORT}/${INDEX_NAME}/language_guesser/${LANG}"


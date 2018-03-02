#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_english_0}
LANGUAGE=${3:-english}
UPDATE_TYPE=${4:-settings}
INDEX_SUFFIX=${5:-""}
curl -v -H "Authorization: Basic $(echo -n 'admin:adminp4ssw0rd' | base64)" \
 -H "Content-Type: application/json" -X PUT "http://localhost:${PORT}/${INDEX_NAME}/${LANGUAGE}/index_management/${UPDATE_TYPE}${INDEX_SUFFIX}"


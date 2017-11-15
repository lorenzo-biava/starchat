#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_0}
curl -v -H "Content-Type: application/json" -X DELETE http://localhost:${PORT}/${INDEX_NAME}/decisiontable/further_details_access_question


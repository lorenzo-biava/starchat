#!/usr/bin/env bash

PORT=${1:-8888}
INDEX_NAME=${2:-index_0}
# retrieve one or more entries with given ids; ids can be specified multiple times
curl -v -H "Content-Type: application/json" "http://localhost:${PORT}/${INDEX_NAME}/decisiontable?ids=further_details_access_question"


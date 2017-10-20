#!/usr/bin/env bash

PORT=${1:-8888}
# retrieve one or more entries with given ids; ids can be specified multiple times
curl -v -H "Content-Type: application/json" "http://localhost:${PORT}/decisiontable?ids=further_details_access_question"


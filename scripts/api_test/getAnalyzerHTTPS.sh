#!/usr/bin/env bash

PORT=${1:-8443}
curl --insecure -v -H "Content-Type: application/json" -X GET "https://localhost:${PORT}/decisiontable_analyzer"


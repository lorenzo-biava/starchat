#!/usr/bin/env bash

PORT=${1:-8888}
curl -v -H "Content-Type: application/json" -X DELETE http://localhost:${PORT}/term -d'{
        "ids": []
}'


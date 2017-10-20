#!/usr/bin/env bash

PORT=${1:-9200}
curl -s -XGET "http://localhost:${PORT}/jenny-en-0/term/_search" -d'{
    "query": {
        "match_all": {}
    }
}'


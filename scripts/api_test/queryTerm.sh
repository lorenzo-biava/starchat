#!/usr/bin/env bash

PORT=${1:-9200}
curl -s -H "Content-Type: application/json" -XGET "http://localhost:${PORT}/index_english_0.term/_search?size=10000" -d'{
    "query": {
        "match_all": {}
    }
}'


#!/usr/bin/env bash

curl -s -XGET "http://localhost:9200/jenny-en-0/term/_search" -d'{
    "query": {
        "match_all": {}
    }
}'


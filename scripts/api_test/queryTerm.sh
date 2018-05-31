#!/usr/bin/env bash

PORT=${1:-9200}
curl -s -H "Content-Type: application/json" -XGET "http://localhost:${PORT}/index_english_common_0.term/_search?size=10000" -d'{
    "query": {
	"match" : { "term.space_punctuation" : "good well" }
    }
}'

#       "match_all": {}


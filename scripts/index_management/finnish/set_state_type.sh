#!/usr/bin/env bash

HOSTNAME=${1:-"localhost"}
PORT=${2:-8000}
INDEX_NAME=${3:-"jenny-fi-0"}
BASEPATH=${4:-""}

if [[ $# -le 1 ]]; then
    echo "Usage: ${0} <hostname> <port> <indexname> <basepath>"
    echo "Esample: ${0} ${HOSTNAME} ${PORT} ${INDEX_NAME} \"/soshojenny\""
    echo "Default: ${0} ${HOSTNAME} ${PORT} ${INDEX_NAME} ${BASENAME}"
    exit 1
fi

echo "Parameters: $@"
# see doc of state machine
# NB queries must be an array of sentences which lead to this state
# see script/apit_test
curl --header "apikey: xxxxxx" -XPUT "${HOSTNAME}:${PORT}${BASEPATH}/${INDEX_NAME}/_mapping/state" -d '
{
	"properties": {
		"state":
		{
			"type": "keyword",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		},
		"action_input":
		{
			"type": "object"
		},
        "state_data":
		{
			"type": "object"
		},
		"queries":
		{
			"type": "text",
			"store": "yes",
			"fields": {
				"raw": {
					"type": "text",
					"index": "not_analyzed"
				},
				"base": {
					"type": "text",
					"analyzer": "ele_base_analyzer"
				},
				"base_bm25": {
					"type": "text",
					"analyzer": "ele_base_analyzer",
					"similarity": "BM25"
				},
				"stop": {
					"type": "text",
					"analyzer": "ele_stop_analyzer"
				},
				"stop_bm25": {
					"type": "text",
					"analyzer": "ele_stop_analyzer",
					"similarity": "BM25"
				},
				"stem": {
					"type": "text",
					"analyzer": "ele_stem_analyzer"
				},
				"stem_bm25": {
					"type": "text",
					"analyzer": "ele_stem_analyzer",
					"similarity": "BM25"
				},
				"shingles_4": {
					"type": "text",
					"analyzer": "ele_shingles_4_analyzer"
				},
				"stemmed_shingles_4": {
					"type": "text",
					"analyzer": "ele_stemmed_shingles_4_analyzer"
				}
			}
		},
		"bubble":
		{
			"type": "text",
			"store": "yes",
			"index": "not_analyzed"
		},
		"action":
		{
			"type": "keyword",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		},
		"success_value":
		{
			"type": "keyword",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		},
		"failure_value":
		{
			"type": "keyword",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		}
	}
}'

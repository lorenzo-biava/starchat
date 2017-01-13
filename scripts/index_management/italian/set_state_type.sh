#!/usr/bin/env bash

HOSTNAME=${1:-"localhost"}
PORT=${2:-8000}
INDEX_NAME=${3:-"jenny-it-0"}
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
	"_timestamp": {
		"enabled": true
	},
	"properties": {
		"state":
		{
			"type": "string",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		},
		"action_input":
		{
			"type": "object"
		},
		"queries":
		{
			"type": "string",
			"store": "yes",
			"fields": {
				"base": {
					"type": "string",
					"analyzer": "ele_base_analyzer"
				},
				"base_lmd": {
					"type": "string",
					"analyzer": "ele_base_analyzer",
					"similarity": "LMDirichlet"
				},
				"stop": {
					"type": "string",
					"analyzer": "ele_stop_analyzer"
				},
				"stop_lmd": {
					"type": "string",
					"analyzer": "ele_stop_analyzer",
					"similarity": "LMDirichlet"
				},
				"stem": {
					"type": "string",
					"analyzer": "ele_stem_analyzer"
				},
				"stem_lmd": {
					"type": "string",
					"analyzer": "ele_stem_analyzer",
					"similarity": "LMDirichlet"
				},
				"shingles_4": {
					"type": "string",
					"analyzer": "ele_shingles_4_analyzer"
				},
				"stemmed_shingles_4": {
					"type": "string",
					"analyzer": "ele_stemmed_shingles_4_analyzer"
				}
			}
		},
		"bubble":
		{
			"type": "string",
			"store": "yes",
			"null_value": ""
		},
		"action":
		{
			"type": "string",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		},
		"success_value":
		{
			"type": "string",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		},
		"failure_value":
		{
			"type": "string",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		}
	}
}'

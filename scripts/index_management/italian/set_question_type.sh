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
# doctype: specify the type of documents
# state: eventual link to any of the state machine states
# verified: was the conversation verified by an operator?
# conversation: ID of the conversation (multiple q&a may be inside a conversation)
# topics: eventually done with LDM or similar
# question: usually what the user of the chat says
# answer: usually what the operator of the chat says
# status: tell whether the document is locked for editing or not, useful for a GUI to avoid concurrent modifications

curl --header "apikey: xxxxxx" -XPUT "${HOSTNAME}:${PORT}${BASEPATH}/${INDEX_NAME}/_mapping/question" -d '
{
	"_timestamp": {
		"enabled": true
	},
	"properties": {
		"doctype":
		{
			"type": "string",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": "hidden"
		},
		"state":
		{
			"type": "string",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": ""
		},
		"verified":
		{
			"type": "boolean",
			"store": true,
			"null_value": false,
			"index": "not_analyzed"
		},
		"conversation":
		{
			"type": "string",
			"index": "not_analyzed",
			"store": "yes"
		},
		"index_in_conversation":
		{
			"type": "integer",
			"store": "yes",
			"null_value": -1
		},
		"topics":
		{
			"type": "string",
			"store": "yes",
			"null_value": "",
			"fields": {
				"base": {
					"type": "string",
					"analyzer": "ele_base_analyzer"
				},
				"base_lmd": {
					"type": "string",
					"analyzer": "ele_base_analyzer",
					"similarity": "LMDirichlet"
				}
			}
		},
		"question":
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
		"answer":
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
		"status": {
			"type": "integer",
			"store": "yes",
			"null_value": 0
		}
	}
}'

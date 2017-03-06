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
	"properties": {
		"doctype":
		{
			"type": "keyword",
			"store": "yes",
			"index": "not_analyzed",
			"null_value": "hidden"
		},
		"state":
		{
			"type": "keyword",
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
			"type": "keyword",
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
			"type": "text",
			"store": "yes",
			"fields": {
				"base": {
					"type": "text",
					"analyzer": "ele_base_analyzer"
				},
				"base_bm25": {
					"type": "text",
					"analyzer": "ele_base_analyzer",
					"similarity": "BM25"
				}
			}
		},
		"question":
		{
			"type": "text",
			"store": "yes",
			"fields": {
				"raw": {
					"type": "keyword"
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
		"answer":
		{
			"type": "text",
			"store": "yes",
			"fields": {
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
		"status": {
			"type": "integer",
			"store": "yes",
			"null_value": 0
		}
	}
}'

#!/usr/bin/env bash

LANG=${1:-en} 
PORT=${2:-8888}
curl -v -H "Content-Type: application/json" -X GET "http://localhost:${PORT}/language_guesser/${LANG}"


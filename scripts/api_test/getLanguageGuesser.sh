#!/usr/bin/env bash

LANG=${1:-en} 
curl -v -H "Content-Type: application/json" -X GET "http://localhost:8888/language_guesser/${LANG}"


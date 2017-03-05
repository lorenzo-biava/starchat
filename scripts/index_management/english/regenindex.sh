#!/usr/bin/env bash

# Remove index if there, otherwise 404
./remove_index.sh localhost 9200 jenny-en-0

# create analyzers in ES (pipeline of analysis of text)
./create_analyzer.sh localhost 9200 jenny-en-0

# Define the schema of question datatype (the one created through
# Crowd Source Data)
./set_question_type.sh localhost 9200 jenny-en-0

# Define schema for the state machine (the spreadsheet)
./set_state_type.sh localhost 9200 jenny-en-0

# Define schema for the state machine (the spreadsheet)
./set_term_type.sh localhost 9200 jenny-en-0


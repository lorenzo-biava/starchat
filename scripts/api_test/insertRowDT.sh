#!/usr/bin/env bash

# state is also used as ID (see updateRowDT.sh)
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/decisiontable -d '{
	"state": "further_details_access_question",
        "max_state_count": 0,
        "execution_order": 0,
        "analyzer": "",
	"queries": ["cannot access account", "problem access account"],
	"bubble": "What seems to be the problem exactly?",
	"action": "show_buttons",
	"action_input": {"Forgot Password": "forgot_password", "Account locked": "account_locked", "Payment problem": "payment_problem", "Specify your problem": "specify_problem", "I want to call an operator": "call_operator", "None of the above": "start"},
    "state_data": {},
	"success_value": "eval(show_buttons)",
	"failure_value": "dont_understand"
}'


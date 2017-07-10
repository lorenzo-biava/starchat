#!/usr/bin/env bash

curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/knowledgebase -d '{
	"id": "0",
	"conversation": "id:1000",
	"index_in_conversation": 1,
	"question": "thank you",
        "question_negative": ["ok, I will not talk with you anymore", "thank you anyway"],
	"answer": "you are welcome!",
	"question_scored_terms": [
		[
			"currently",
			1.0901874131103333
		],
		[
			"installing",
			2.11472759638322
		],
		[
			"mac",
			9.000484252244254
		],
		[
			"reset",
			4.34483238516225
		],
		[
			"app",
			1.2219061535961406
		],
		[
			"device",
			2.1679468390743414E-213
		],
		[
			"devices",
			4.1987625801077624E-268
		]
	],
	"verified": true,
	"topics": "t1 t2",
	"doctype": "normal",
	"state": "",
	"status": 0
}' 

curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/knowledgebase -d '{
	"id": "1",
	"conversation": "id:1000",
	"index_in_conversation": 1,
	"question": "how are you?",
        "question_negative": ["are you kidding me?"],
	"answer": "fine thanks",
	"question_scored_terms": [
		[
			"validation",
			0.03431486996831187
		],
		[
			"imac",
			1.1298276004683466
		],
		[
			"aware",
			3.1504895812959743
		],
		[
			"ios",
			6.14545226791214
		],
		[
			"activation",
			4.921338043098873
		]
	],
	"verified": true,
	"topics": "t1 t2",
	"doctype": "normal",
	"state": "",
	"status": 0
}' 

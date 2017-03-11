# Welcome!
This is the official repository for the *Chat, a scalable conversational engine for B2B applications.

# How to contribute

To contribute to *Chat, please send us a [pull request](https://help.github.com/articles/using-pull-requests/#fork--pull) 
from your fork of this repository!

Our concise [contribution guideline](https://github.com/GetJenny/starchat/blob/master/CONTRIBUTING.md) contains the bare
minumum requirements of the code contributions.

Before contributing (or opening issues), you might want send us an email at starchat@getjenny.com.

   * [Quick Start](#quick-start)
      * [Requirements](#requirements)
      * [Setup with Docker (recommended)](#setup-with-docker-recommended)
      * [Install without Docker](#install-without-docker)
      * [Test the installation](#test-the-installation)
      * [*Chat in brief](#chat-in-brief)
   * [Technology](#technology)
      * [How does *Chat work?](#how-does-chat-work)
      * [Configuration of the DecisionTable](#configuration-of-the-decisiontable)
      * [Client functions](#client-functions)
      * [Mechanics](#mechanics)
   * [APIs](#apis)
   * [Test](#test)
   * [Troubleshooting](#troubleshooting)
      * [Size of the virtual memory](#size-of-the-virtual-memory)


# Quick Start

## Requirements

The easiest way is to install *chat using two docker images. You only need:

* [sbt](http://www.scala-sbt.org)
* [docker](https://docs.docker.com/engine/installation/)
* [docker compose](https://docs.docker.com/compose/install/)

In this way, you will put all the indices in the Elasticsearch (V 5.2) image, and *chat itself in the Java (8) image.

_If you do not use docker_ you therefore need on your machine:

1. [Scala 12.2](http://scala-lang.org)
2. [Elasticsearch 5.2](http://elastic.co)

## Setup with Docker (recommended)

Generate a packet distribution: 
```bash
sbt dist
```

Enter the directory docker-starchat:
```bash
cd  docker-starchat
```
Extract the packet into the docker-starchat folder: 
```bash
unzip ../target/universal/starchat-master.zip
```

Review the configuration files `starchat-master/config/application.conf`:
- check that IP and Port for elasticsearch are correct.
- configure the language

Start both startchat and elasticsearch: 
```bash
docker-compose up -d
```

(Problems like `elastisearch exited with code 78`? have a look at [troubleshooting](#troubleshooting)!)

Now you have an empty installation of `*chat`. You need to initialize Elasticsearch and then configure the bot.

### Prepare ElasticSearch indices

* enter the directory scripts/api_test
* run `./postIndexManagement.sh`

###Configure *chat

Now you have to load the configuration file for the actual chat. We have provided an example csv in English, therefore:

* `cd /scripts/indexing`
* `./index_documents_dt.py ../../doc/sample_state_machine_specification.csv 1`

##Install without Docker
 
* Clone the repository and enter the starchat directory.
* Initialize the elasticsearch instance with `regenindex.sh` (see above for Docker)
* Run the service: `sbt compile run`

The service binds on the port 8888 by default.

##Test the installation

Is the service working?

`curl -X GET localhost:8888 | python -mjson.tool`

Tell *Chat you cannot access your account:

```bash
curl  -H "Content-Type: application/json" -X POST http://localhost:8888/get_next_response -d '{   
 "conversation_id": "1234",   
 "user_input": { "text": "I forgot my password" },   
 "values": {
   "return_value": "", 
   "data": {}   
 }
}'
```

You should get something like:

```json
{
    "conversation_id": "1234",   
    "action": "input_form",
    "action_input": {
        "email": "email"
    },
    "bubble": "We can reset your password by sending you a message to your registered e-mail address. Please tell me your address so I may send you the new password generation link.",
    "data": {},
    "failure_value": "\"dont_understand\"",
    "max_state_count": 0,
    "analyzer": "",
    "state": "forgot_password",
    "state_data": {
        "verification": "did you mean you forgot the password?"
    },
    "success_value": "\"send_password_generation_link\""
}
```

Which means *Chat understood your problem and went to the `forgot_password` state. 

## *Chat in brief

With *Chat you can easily implement workflow-based chatbots. Its strength are:

* Simple yet powerful configuration file
* Easy installation
* Easy integration with any client (Slack, Telegram etc)

*Chat allows a developer to create a conversation flow through –

* identification of the best state to start from based on the language analysis of what the user wrote
* creation of dynamic content using variables inferred from the conversation (e.g. "Please write your email so that I can send you a message")

# Technology

*Chat was design with the following goals in mind:

0. easy deployment
1. horizontally scalability without any service interruption.
2. modularity
3. statelessness

## How does *Chat work?

### Workflow

![alt tag](https://www.websequencediagrams.com/cgi-bin/cdraw?lz=dGl0bGUgc2ltcGxpZmllZCBSZXN0QVBJQ2FsbGluZ01lY2hhbmlzbSBpbiAqQ2hhdAoKVXNlciAtPiBTdGFyY2hhdFJlc291cmNlOiByZXN0IGFwaSBjYWxsIChpbiBqc29uKQoAGhAAKBZqc29uIGRlc2VyaWFsaXphdGlvbiBpbnRvIGVudGl0eQAqHVNlcnZpY2U6AHYFaW5nIGZ1bmMAPQUoaW4AOgcAgH8KACcHACwVADAJZXhlY3V0aW9uABscAIFzCgBoCXJlc3VsdCAob3V0AGgRAIFkHgCBbQ5vZgCBcgcAgX8GamVzAIEACwCCPwxVc2VyAIJyC3Jlc3BvbnNlAHwGAIJ8BgoK&s=napkin)

### Components

*Chat uses Elasticsearch as database. In addition, it also leverage on Elasticsearch NLP capabilities, using
its smart indexing system, sentence cleansing, tokenization etc

### Services

*Chat consists of two different services: the "KnowledBase" and the "DecisionTable"

#### KnowledgeBase

For quick setup based on real Q&A logs. It stores question and answers pairs. Given a text as input
 it proposes the pair with the closest match on the question field. 
 At the moment the KnowledBase supports only Analyzers implemented on Elasticsearch.

#### DecisionTable

The conversational engine itself. For the usage, see below.

## Configuration of the DecisionTable

You configure the DecisionTable through CSV file. Please have a look at the one provided in `doc/`:

|state|max_state_count|analyzer|queries |bubble|action|action_input|state_data|success_value |failure_value|
|-----|---------------|-----|--------|------|------|------------|----------|--------------|-------------|
|start|0              |     |      |"How may I help you?"||||||
|further_details_access_question|0|((forgot).*(password))|"[""cannot access account"", ""problem access account""]"||show_buttons|"{""Forgot Password"": ""forgot_password"", ""Account locked"": ""account_locked"", ""None of the above"": ""start""}"||eval(show_buttons),"""dont_understand"""|
|forgot_password|0||"[""Forgot password""]"|"I will send you a new password generation link, enter your email."|input_form|"{""email"": ""email""}"||"""send_password_generation_link"""|"""dont_understand"""|
|send_password_generation_link|0|||"Sending message to %email% with instructions."|send_password_generation_link|"{ ""template"": "If you requested a password reset, follow this link: %link%"", ""email"": ""%email%"" }"||"""any_further"""|call_operator|


Fields in the configuration file are of three types:

* **(R)**: Return value: the field is returned by the API
* **(T)**: Triggers to the state: when should we enter this state? 
* **(I)**: Internal: a field not exposed to the API

And the fields are:

* **state**: a unique name of the state (e.g. `forgot_password`)
* **max_state_count**: defines how many times *Chat can repropose the state during a conversation.
* **analyzer**: specify an analyzer expression which triggers the state
* **query (T,I)**: list of sentences whose meaning identify the state
* **bubble (R)**: content, if any, to be shown to the user. It may contain variables like %email% or %link%.
* **action (R)**: a function to be called on the client side. *Chat developer must provide types of input and output (like an abstract method), and the GUI developer is responsible for the actual implementation (e.g. `show_button`)
* **action_input (R)**: input passed to **action**'s function (e.g., for `show_buttons` can be a list of pairs `("text to be shown on button", state_to_go_when_clicked)` 
* **state_data (R)**: a dictionary of strings with arbitrary data to pass along
* **success_value (R)**: output to return in case of success
* **failure_value (R)**: output to return in case of failure

## Client functions

For the CSV in the example above, the client will have to implement the following set of functions:

* show_buttons: tell the client to render a multiple choice button
    * input: a key/value pair with the key indicating the text to be shown in the button, and the value indicating the state to follow e.g.: {"Forgot Password": "forgot_password", "Account locked": "account_locked", "Specify your problem": "specify_problem", "I want to call an operator": "call_operator", "None of the above": "start"}
    * output: the choice related to the button clicked by the user e.g.: "account_locked"
* input_form: render an input form or collect the input following a specific format
    * input: a dictionary with the list of fields and the type of fields, at least "email" must be supported: e.g.: { "email": "email" } where the key is the name and the value is the type
    * output: a dictionary with the input values e.g.: { "email": "foo@example.com" }
* send_password_generation_link: send an email with instructions to regenerate the password
    * input: a valid email address e.g.: "foo@example.com"
    * output: a dictionary with the response fields e.g.: { "user_id": "123", "current_state": "forgot_password", "status": "true" }

Other application specific functions can be implemented by the client these functions must be called with the prefix
"priv_" e.g. "priv_retrieve_user_transactions" ( @angleto to clarify)

Ref: [sample_state_machine_specification.csv](https://github.com/GetJenny/starchat/blob/master/doc/sample_state_machine_specification.csv).

## Mechanics

* The client implements the functions which appear in the action field of the spreadsheet. 
We will provide interfaces.
* The client call the rest API "decisiontable" endpoint communicating a state if any, 
the user input data and other state variables
* The client receive a response with guidance on what to return to the user and what 
are the possible next steps
* The client render the message to the user and eventually collect the input, then 
call again the system to get instructions on what to do next
* When the "decisiontable" functions does not return any result the user can call the "knowledgebase" endpoint
which contains all the conversations. 
  
# APIs

## `POST /get_next_response` 

Tell *Chat about the user actions (wrote something, clicked a button etc) and receives instruction 
about the next state.

Data to post:

```json
{
    "conversation_id": "1234",
    "user_input": "(Optional)",
    "text" : "the text typed by the user (Optional)",
    "img": "(e.g.) image attached by the user (Optional)",
    "values": "(Optional)",
    "return_value": "the value either in success_value or in failure_value (Optional)",
    "data": "all the variables, e.g. for the STRING TEMPLATEs (Optional)"
}
```
### Return codes

####200

Similar Json, see examples below

##### Example 1

User input is "I forgot my password":

```bash
curl  -H "Content-Type: application/json" -X POST http://localhost:8888/get_next_response -d '{   
"conversation_id": "1234",   
"user_input": { "text": "I forgot my password" },   
"values": {
    "return_value": "", 
    "data": {}   
    }
}'
```

returns:

```json
{
    "action": "input_form",
    "action_input": {
        "email": "email"
    },
    "bubble": "We can reset your password by sending you a message to your registered e-mail address. Please tell me your address so I may send you the new password generation link.",
    "conversation_id": "1234",
    "data": {},
    "failure_value": "\"dont_understand\"",
    "max_state_count": 0,
    "analyzer": "",
    "state": "forgot_password",
    "state_data": {
        "verification": "did you mean you forgot the password?"
    },
    "success_value": "\"send_password_generation_link\""
}
```

##### Example 2

User inserts their email after having been in `forgot_password`. 
The client sends:

```bash
curl  -H "Content-Type: application/json" -X POST http://localhost:8888/get_next_response -d '
{
    "conversation_id": "1234",
    "user_input": { "text": "" },
    "values": {
        "return_value": "send_password_generation_link",
        "data": { "email": "john@example.com" }
    }
}'
```
and gets:

```json
{
    "action": "send_password_generation_link",
    "action_input": {
        "email": "john@example.com",
        "template": "somebody requested to reset your password, if you requested the password reset follow the link: %link%"
    },
    "bubble": "Thank you. An e-mail will be sent to this address: a@b.com with your account details and the necessary steps for you to reset your password.",
    "conversation_id": "1234",
    "data": {
        "email": "john@example.com"
    },
    "failure_value": "call_operator",
    "max_state_count": 0,
    "analyzer": "",
    "state": "send_password_generation_link",
    "state_data": {},
    "success_value": "\"any_further\""
}

```

#### 204

No response was found

#### 500 (error)

Internal server error

#### 400 (error)

Bad request: 

    * meaning: the input data structure is not valid
    * output data: no data returned

#### 422 (error)

    * meaning: bad request data, the input data is formally valid but there is some issue with data interpretation
    * output data: the output data structure is a json dictionary with two fields: code and message. The following code are supported:
        * code: 100
        * message: "error evaluating the template strings, bad values"

#### 404 (error)

    * meaning: not found
    * output data: no data returned

## `GET /decisiontable` 

Get a document by ID

Output JSON

### Return codes 

#### 200

Sample call

```bash
# retrieve one or more entries with given ids; ids can be specified multiple times
curl -v -H "Content-Type: application/json" "http://localhost:8888/decisiontable?ids=further_details_access_question"
```

Sample output

```json
{
  "total": 1,
  "max_score": 0,
  "hits": [
    {
      "score": 0,
      "document": {
        "analyzer": "((forgot).*(password))",
        "queries": [
          "cannot access account",
          "problem access account"
        ],
        "state": "further_details_access_question",
        "state_data": {
          "verification": "did you mean you can't access to your account?"
        },
        "success_value": "eval(show_buttons)",
        "failure_value": "\"dont_understand\"",
        "bubble": "Hello and welcome to our customer service chat. Please note that while I am not a human operator, I will do my very best to assist You today. How may I help you?",
        "action_input": {
          "Specify your problem": "specify_problem",
          "I want to call an operator": "call_operator",
          "None of the above": "start",
          "Forgot Password": "forgot_password",
          "Account locked": "account_locked"
        },
        "max_state_count": 0,
        "action": "show_buttons"
      }
    }
  ]
}
```

## `PUT /decisiontable`
 
Output JSON

### Return codes

#### 201

Sample call

```bash
# update the "further_details_access_question" entry in the DT
curl -v -H "Content-Type: application/json" -X PUT http://localhost:8888/decisiontable/further_details_access_question -d '{
  "queries": ["cannot access account", "problem access account", "unable to access to my account"]
}'
```

Sample output
```json
{
    "created": false,
    "dtype": "state",
    "id": "further_details_access_question",
    "index": "jenny-en-0",
    "version": 2
}
```

## `POST /decisiontable`

Insert a new document.

Output JSON

### Return codes

#### 201

Sample call

```bash
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/decisiontable -d '{
  "state": "further_details_access_question",
  "max_state_count": 0,
  "analyzer": "",
  "queries": ["cannot access account", "problem access account"],
  "bubble": "What seems to be the problem exactly?",
  "action": "show_buttons",
  "action_input": {"Forgot Password": "forgot_password", "Account locked": "account_locked", "Payment problem": "payment_problem", "Specify your problem": "specify_problem", "I want to call an operator": "call_operator", "None of the above": "start"},
  "success_value": "eval(show_buttons)",
  "failure_value": "dont_understand"
}'
```

Sample output

```json
{
    "created": true,
    "dtype": "state",
    "id": "further_details_access_question",
    "index": "jenny-en-0",
    "version": 1
}
```

## `DELETE /decisiontable`

Delete a document by ID

Output JSON

### Return codes 

#### 200

Sample call
```bash
curl -v -H "Content-Type: application/json" -X DELETE http://localhost:8888/decisiontable/further_details_access_question
```

Sample output

```json
{
    "dtype": "state",
    "found": true,
    "id": "further_details_access_question",
    "index": "jenny-en-0",
    "version": 3
}
```

## `POST /decisiontable_search`

Update a document

Output JSON

### Return codes 

#### 200

Sample call
```bash
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/decisiontable_search -d '{
  "queries": "cannot access my account",
  "min_score": 0.1,
  "boost_exact_match_factor": 2.0
}'
```

## `GET /decisiontable_analyzer` 

(WORK IN PROGRESS, PARTIALLY IMPLEMENTED)

Get and return the map of analyzer for each state

Output JSON

### Return codes 

#### 200

Sample call
```bash
curl -v -H "Content-Type: application/json" -X GET "http://localhost:8888/decisiontable_analyzer"
```

Sample response

```json
{
  "analyzer_map": {
    "further_details_access_question": "((forgot).*(password))"
  }
}
```

## `POST decisiontable_analyzer`

Load/reload the map of analyzer from ES

Output JSON

### Return codes 

#### 200

Sample call
```bash
curl -v -H "Content-Type: application/json" -X POST "http://localhost:8888/decisiontable_analyzer"
```

Sample response

```json
{"num_of_entries":1}
```

## `GET /knowledgebase`

Return a document by ID

Output JSON

### Return codes 

#### 200

Sample call
```bash
# retrieve one or more entries with given ids; ids can be specified multiple times
curl -v -H "Content-Type: application/json" "http://localhost:8888/knowledgebase?ids=0"
```

Sample response

```json
{
    "hits": [
        {
            "document": {
                "answer": "you are welcome!",
                "conversation": "832",
                "doctype": "normal",
                "id": "0",
                "index_in_conversation": 11,
                "question": "thank you",
                "state": "",
                "status": 0,
                "topics": "",
                "verified": false
            },
            "score": 0.0
        }
    ],
    "max_score": 0.0,
    "total": 1
}
```

## `POST knowledgebase`

Insert a new document

Sample call

Output JSON

### Return codes 

#### 201

```bash
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/starchat-en/knowledgebase -d '{
    "answer": "you are welcome!",
    "conversation": "832",
    "doctype": "normal",
    "id": "0",
    "index_in_conversation": 11,
    "question": "thank you",
    "state": "",
    "status": 0,
    "topics": "",
    "verified": true
}'
```

Sample response

```json
{
    "hits": [
        {
            "document": {
                "answer": "you are welcome!",
                "conversation": "832",
                "doctype": "normal",
                "id": "0",
                "index_in_conversation": 11,
                "question": "thank you",
                "state": "",
                "status": 0,
                "topics": "",
                "verified": true
            },
            "score": 0.0
        }
    ],
    "max_score": 0.0,
    "total": 1
}
```

## `DELETE /knowledgebase`

Delete a document by ID

Output JSON

### Return codes 

#### 200

Sample call

curl -v -H "Content-Type: application/json" -X DELETE http://localhost:8888/knowledgebase/0

Sample output

```bash
{
    "dtype": "question",
    "found": false,
    "id": "0",
    "index": "jenny-en-0",
    "version": 5
}
```

## `PUT knowledgebase`

Update an existing document

Output JSON

### Return codes 

#### 200

Sample call

```bash
curl -v -H "Content-Type: application/json" -X PUT http://localhost:8888/starchat-en/knowledgebase/                                                   e9d7c04d0c539415620884f8c885fef93e9fd0b49bbea23a7f2d08426e4d185119068365a0c1c4a506c5c43079e1e8da4ef7558a7f74756a8d850cb2d14e5297 -d '{
    "answer": "you are welcome!",
    "conversation": "832",
    "doctype": "normal",
    "index_in_conversation": 11,
    "question": "thank yoy",
    "state": "",
    "status": 0,
    "topics": "",
    "verified": false
}'
```

Sample response

```json
{
    "created": false,
    "dtype": "question",
    "id": "e9d7c04d0c539415620884f8c885fef93e9fd0b49bbea23a7f2d08426e4d185119068365a0c1c4a506c5c43079e1e8da4ef7558a7f74756a8d850cb2d14e5297",
    "index": "jenny-en-0",
    "version": 3
}
```

## `POST knowledgebase_search`

Output JSON

### Return codes 

#### 200

Sample call

```bash
curl -v -H "Content-Type: application/json" -X POST http://localhost:8888/knowledgebase_search -d '{
  "question": "thank you",
  "verified": true,
  "doctype": "normal"
}'
```

Sample output

```json
{
    "hits": [
        {
            "document": {
                "answer": "you are welcome",
                "conversation": "4346",
                "doctype": "normal",
                "id": "10",
                "index_in_conversation": 6,
                "question": "thank you",
                "state": "",
                "status": 0,
                "topics": "",
                "verified": true
            },
            "score": 3.5618982315063477
        }
    ],
    "max_score": 3.5618982315063477,
    "total": 1
}
```

# Test

* Unit tests are available with `sbt test` command
* A set of test script is present inside scripts/api_test

# Troubleshooting

## Size of the virtual memory

If elasticsearch complain about the size of the virtual memory:
```
max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]
elastisearch exited with code 78
```

run:

```bash
sysctl -w vm.max_map_count=262144
```

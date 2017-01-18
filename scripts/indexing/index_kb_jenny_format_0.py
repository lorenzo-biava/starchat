#!/usr/bin/env python

import sys
import csv
import interface
import hashlib
import json
import base64

base_url = "http://0.0.0.0:8888"
service_url = base_url + ""


service = interface.Service()
service.service_url = service_url
service.post_headers = {'Content-Type': 'application/json', 'apikey': 'xxxxxx'}
service.get_headers = service.post_headers


def load_data(questions_file, answers_file, associations_file):
    with open(questions_file) as qf, open(answers_file) as af, open(associations_file) as aaf:
        questions = csv.reader(qf, delimiter=';', quotechar='"')
        answers = csv.reader(af, delimiter=';', quotechar='"')
        associations = csv.reader(aaf, delimiter=';', quotechar='"')

        questions_dict = {}
        next(questions)
        for row in questions:
            questions_dict[row[0]] = row[1]

        answers_dict = {}
        next(answers)
        for row in answers:
            answers_dict[row[0]] = row[1]

        associations_dict = {}
        next(associations)
        for row in associations:
            entry = { "conversation": row[1], "question": row[0], "position": int(row[2]), "answer": row[3]}
            associations_dict[row[0]] = entry

        data = { "questions_dict": questions_dict, "answers_dict": answers_dict, "associations_dict": associations_dict }
        return data


def load_data_base64(questions_file, answers_file, associations_file):
    with open(questions_file) as qf, open(answers_file) as af, open(associations_file) as aaf:
        questions = csv.reader(qf, delimiter=';')
        answers = csv.reader(af, delimiter=';')
        associations = csv.reader(aaf, delimiter=';')

        questions_dict = {}
        #next(questions)
        for row in questions:
            questions_dict[row[0]] = base64.b64decode(row[1]).decode("utf-8")

        answers_dict = {}
        #next(answers)
        for row in answers:
            answers_dict[row[0]] = base64.b64decode(row[1]).decode("utf-8")

        associations_dict = {}
        next(associations)
        for row in associations:
            entry = { "conversation": row[1], "question": row[0], "position": int(row[2]), "answer": row[3]}
            associations_dict[row[0]] = entry

        data = { "questions_dict": questions_dict, "answers_dict": answers_dict, "associations_dict": associations_dict }
        return data


def join_data(questions_dict, answers_dict, associations_dict):
    conversations = {}
    for k, v in associations_dict.items():
        conv_n = v["conversation"]
        position_n = v["position"]
        answer_n = v["answer"]
        question_n = v["question"]
        try:
            entry = { "conversation": conv_n, "position": position_n, "question": questions_dict[question_n], "answer": answers_dict[answer_n] }
        except:
            print("Error: ", k, v)
            continue
        yield(entry)


questions_path = sys.argv[1]
answers_path = sys.argv[2]
associations_path = sys.argv[3]

input_in_base64 = False
if(len(sys.argv) >= 5):
    input_in_base64 = True

if(input_in_base64):
    data = load_data_base64(questions_path, answers_path, associations_path)
else:
    data = load_data(questions_path, answers_path, associations_path)

joined_data = join_data(data["questions_dict"], data["answers_dict"], data["associations_dict"])

for item in joined_data:
    attempts = 10
    while attempts > 0:
        try:
            print("indexing document: ", item)
            the_id = hashlib.sha512(str(item).encode()).hexdigest()
            res = service.index_document_kb(the_id=the_id, conversation=item["conversation"], index_in_conversation=item["position"],
                    question=item["question"], answer=item["answer"], verified=False, topics="", doctype="normal", state="", status=0)
        except interface.ApiCallException as exc:
            print("Error, res: ", res)
        if res[0] > 299 or res[0] < 200:
            print("error, retrying: ", item)
            attempts -= 1
            continue
        else:
            attempts = 0


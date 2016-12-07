#!/usr/bin/env python3

import sys
import csv
import interface
import hashlib
import json

base_url = "http://0.0.0.0:8888"
service_url = base_url + ""


service = interface.Service()
service.service_url = service_url
service.post_headers = {'Content-Type': 'application/json', 'apikey': 'xxxxxx'}
service.get_headers = service.post_headers

def index_items(item_listfile, skiplines=1):
    lcounter = 0
    with open(item_listfile, 'r', encoding="utf-8") as items_fd:
        freader = csv.reader(items_fd, delimiter=',', quotechar='"')
        i = 0
        while i < skiplines:
            items_fd.readline()
            i += 1
        lcounter += skiplines
        for row in freader:
            i += 1
            attempts = 10

            state = row[0]
            if row[1]:
                try:
                    queries = json.loads(row[1])
                except:
                    print("Error: row[1]", i, row[1])
                    sys.exit(1)
            else:
                queries = []
            bubble = row[2]
            action = row[3]
            if row[4]:
                try:
                    action_input = json.loads(row[4])
                except:
                    print("Error: row[4]", i, row[4])
                    sys.exit(4)
            else:
                action_input = {}
            success_value = row[5]
            failure_value = row[6]

            while attempts > 0:
                try:
                    res = service.index_document_dt(state, queries, bubble, action, action_input, success_value, failure_value)
                except interface.ApiCallException as exc:
                    print("Last line: ", lcounter)
                    sys.exit(1)

                if res[0] > 299 or res[0] < 200:
                    print("error, retrying: ", row)
                    attempts -= 1
                    continue
                else:
                    attempts = 0

                lcounter += 1
                print("indexed document: ", row)

item_listfile = sys.argv[1]
skiplines = int(sys.argv[2])

index_items(item_listfile=item_listfile, skiplines=skiplines)

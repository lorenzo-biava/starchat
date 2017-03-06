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
service.post_headers = {'Content-Type': 'application/json', 'apikey': 'xxxx'}
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
            max_state_count = int(row[1])
            regex = row[2]
            if row[3]:
                try:
                    queries = json.loads(row[3])
                except:
                    print("Error: row[1]", i, row[3])
                    sys.exit(1)
            else:
                queries = []
            bubble = row[4]
            action = row[5]
            if row[6]:
                try:
                    action_input = json.loads(row[6])
                except:
                    print("Error: row[4]", i, row[6])
                    sys.exit(4)
            else:
                action_input = {}
            if row[7]:
                try:
                    state_data = json.loads(row[7])
                except:
                    print("Error: row[7]", i, row[7])
                    sys.exit(4)
            else:
                state_data = {}
            success_value = row[8]
            failure_value = row[9]

            while attempts > 0:
                try:
                    res = service.delete_document_dt(item_id=state)
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
                print("deleted document: ", row)

item_listfile = sys.argv[1]
skiplines = int(sys.argv[2])

index_items(item_listfile=item_listfile, skiplines=skiplines)

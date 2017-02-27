#!/usr/bin/env python3

from urllib3 import PoolManager, Timeout
import json
import time


class ApiCallException(Exception):
    """
    Exception used to report problems inserting data on graph
    """
    def __init__(self, value):
        self.value = value

    def __str__(self):
        return repr(self.value)


class Service:
    def __init__(self):
        base_url = "http://localhost:8000"
        self.service_url = base_url + "/starchat"
        self.post_headers = {'Content-Type': 'application/json', 'apikey': 'xxxxxx'}
        self.get_headers = self.post_headers

    @staticmethod
    def call_api_function(url, method, body=None, headers={'Content-Type': 'application/json'}):
        """
        call an API's function and return response

        exception: raise an exception if any error occur

        :param url: the url
        :param method: POST or GET, DELETE
        :param body: the body of the request if any
        :return: the data structure or None
        """
        try:
            with PoolManager(retries=5, timeout=Timeout(total=5.0)) as http:
                r = http.urlopen(method, url, headers=headers, body=body)
                ret_data = (r.status, r.data)
                r.close()
        except Exception as exc:
            e_message = "error getting response from url: " + url
            raise ApiCallException(e_message)
        else:
            if ret_data:
                try:
                    print(ret_data[1])
                    structured_res = (ret_data[0], json.loads(ret_data[1].decode("utf-8")))
                except ValueError as exc:
                    e_message = str(ret_data[0]) + " : error parsing response from url: " + url
                    raise ApiCallException(e_message)
            else:
                structured_res = None
            return structured_res

    def index_document_dt(self, state, max_state_count, regex, queries, bubble, action, action_input, state_data,
                          success_value, failure_value):
        url = self.service_url + "/decisiontable"
        headers = self.post_headers
        body = {
            "state": state,
            "max_state_count": max_state_count,
            "regex": regex,
            "queries": queries,
            "bubble": bubble,
            "action": action,
            "action_input": action_input,
            "state_data": state_data,
            "success_value": success_value,
            "failure_value": failure_value
        }
        res = self.call_api_function(url=url, method="POST", body=json.dumps(body), headers=headers)
        if res[0] > 299 or res[0] < 200:
            print("Error: indexed_doc (DT) :", state, res)
        return res

    def delete_document_dt(self, item_id):
        url = self.service_url + "/decisiontable/" + item_id
        headers = self.post_headers
        res = self.call_api_function(url=url, method="DELETE", body=None, headers=headers)
        if res[0] > 299 or res[0] < 200:
            print("Error: removing (DT) :", item_id, res)
        return res

    def index_document_kb(self, the_id, conversation, question, answer, index_in_conversation=-1, verified=False, topics="", doctype="normal", state="", status=0):
        url = self.service_url + "/knowledgebase"
        headers = self.post_headers
        body = {
            "id": the_id,
            "conversation": conversation,
            "index_in_conversation": index_in_conversation,
            "question": question,
            "answer": answer,
            "verified": verified,
            "topics": topics,
            "doctype": doctype,
            "state": state,
            "status": status
        }

        print(body)
        res = self.call_api_function(url=url, method="POST", body=json.dumps(body), headers=headers)
        if res[0] > 299 or res[0] < 200:
            print("Error: indexed_doc (KB) :", state, res)
        return res

    def delete_document_kb(self, item_id):
        url = self.service_url + "/knowledgebase/" + item_id
        headers = self.post_headers
        res = self.call_api_function(url=url, method="DELETE", body=None, headers=headers)
        if res[0] > 299 or res[0] < 200:
            print("Error: removing (KB) :", item_id, res)
        return res


#!/usr/bin/env python
# -*- coding: utf-8 -*-

import classify_image
import tensorflow as tf
import boto3
import os
import time
import sys
import json

import numpy as np
from six.moves import urllib
import urllib2

try:
    import constants
except ImportError:
    constants = object()

SQS_REQUEST_QUEUE_NAME = 'request'
SQS_RESPONSE_QUEUE_NAME = 'response'
SQS_TERMINATE_QUEUE_NAME = 'terminate'

S3_UPLOAD_ENABLED = getattr(constants, 'S3_UPLOAD_ENABLED', True)
S3_BUCKET_NAME = getattr(constants, 'S3_BUCKET_NAME', 'cse546-proj1-results')
PROFILE_NAME = getattr(constants, 'PROFILE_NAME', 'default')
TERMINATE_REQUEST_EXPIRES_IN = int(getattr(constants, 'TERMINATE_REQUEST_EXPIRES_IN', 4000))
TERMINATE_CHECK_INTERVAL = int(getattr(constants, 'TERMINATE_CHECK_INTERVAL', 5))

session = boto3.Session(profile_name=PROFILE_NAME)
AWS_ACCESS_KEY_ID = session.get_credentials().access_key
AWS_SECRET_ACCESS_KEY = session.get_credentials().secret_key
AWS_REGION_NAME = session.region_name

MODEL_DIR = '/home/ubuntu/imagenet'

class FLAGS:
    model_dir = MODEL_DIR

classify_image.FLAGS = FLAGS

classify_image.create_graph()

class Worker(object):

    def download_image(self, image_url):
        req = urllib2.Request(image_url, headers={'User-Agent' : "Magic Browser"})
        response = urllib.request.urlopen(req)
        return response.read()

    def run(self):
        sqs = session.resource('sqs', 'us-west-1')
        s3 = session.resource('s3')

        request_queue = sqs.get_queue_by_name(QueueName=SQS_REQUEST_QUEUE_NAME)
        response_queue = sqs.get_queue_by_name(QueueName=SQS_RESPONSE_QUEUE_NAME)
        terminate_queue = sqs.get_queue_by_name(QueueName=SQS_TERMINATE_QUEUE_NAME)
        bucket = s3.Bucket(S3_BUCKET_NAME)

        # Creates node ID --> English string lookup.
        node_lookup = classify_image.NodeLookup()

        request_count = 0
        with tf.Session() as sess:
            softmax_tensor = sess.graph.get_tensor_by_name('softmax:0')

            check = False

            while True:

                if check:
                    check = False
                    request_count = 0
                    while True:
                        for message in terminate_queue.receive_messages(MaxNumberOfMessages=1, AttributeNames=['SentTimestamp']):
                            message.delete()
                            message_ts = int(message.attributes['SentTimestamp'])
                            current_ts = int(time.time()*1000)
                            print current_ts, message_ts
                            if (current_ts - message_ts) <= TERMINATE_REQUEST_EXPIRES_IN:
                                # Valid termination request
                                return
                            else:
                                # Expired request
                                break
                        else:
                            break

                processed = False
                for message in request_queue.receive_messages(WaitTimeSeconds=20,
                                                              MaxNumberOfMessages=1):
                    request = json.loads(message.body)
                    message.delete()

                    image_url = request['imageUrl']
                    image_data = self.download_image(image_url)
                    predictions = sess.run(softmax_tensor,
                                           {'DecodeJpeg/contents:0': image_data})
                    predictions = np.squeeze(predictions)
                    node_id = predictions.argsort()[-1]
                    human_string = node_lookup.id_to_string(node_id)

                    print image_url, human_string

                    request['result'] = human_string
                    response_queue.send_message(MessageBody=json.dumps(request))

                    if S3_UPLOAD_ENABLED:
                        image_name = os.path.basename(image_url)
                        while True:
                            try:
                                bucket.put_object(Key=image_name, Body=human_string)
                                break
                            except Exception:
                                bucket = s3.Bucket(S3_BUCKET_NAME)
                                time.sleep(2)

                    processed = True
                    request_count += 1

                if not processed:
                    check = True
                elif request_count and (request_count % TERMINATE_CHECK_INTERVAL == 0):
                    check = True



def main():
    worker = Worker()
    while True:
        try:
            worker.run()
        except Exception:
            continue
    os.system("ec2-terminate-instances $(curl -s 'http://169.254.169.254/latest/meta-data/instance-id') -O '{}' -W '{}' --region '{}'".format(AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION_NAME))
    sys.exit(0)

if __name__ == '__main__':
    main()

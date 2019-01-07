#!/usr/bin/env bash

source /home/ubuntu/tensorflow/bin/activate

exit_code=1
while [ $exit_code ];
do
    python /home/ubuntu/proj1-worker/worker.py
    exit_code=$?
done

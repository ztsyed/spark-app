#!/bin/bash -e
/spark-1.3.1/bin/spark-submit --class org.apache.spark.examples.streaming.OpenCVFeatureCount target/spk-app-1.0.jar nats://127.0.0.1:4222

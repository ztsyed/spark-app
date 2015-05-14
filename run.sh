#!/bin/bash -e
/spark-1.3.1/bin/spark-submit --class com.erix.streaming.OpenCVFeatureCount target/spark-app-1.0.jar nats://127.0.0.1:4222

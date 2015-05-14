#!/bin/bash -e
/spark-1.3.1/bin/spark-submit --class com.erix.streaming.OpenCVFeatureCount target/spark-app-1.0.jar nats://192.168.45.15:4222

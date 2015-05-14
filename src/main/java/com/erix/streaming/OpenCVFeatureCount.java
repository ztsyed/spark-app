/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.erix.streaming;
import java.util.Arrays;
import scala.Tuple2;
import com.google.common.collect.Lists;
import com.google.common.base.Optional;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.Function;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.spark.streaming.Time;
import java.io.IOException;
import org.apache.spark.HashPartitioner;
/**
 * Counts words in UTF8 encoded, '\n' delimited text received from the network every second.
 *
 * Usage: OpenCVFeatureCount <nats url>
 *
 *    `$ bin/run-example org.apache.spark.examples.streaming.OpenCVFeatureCount nats://127.0.0.1:4222`
 */
public final class OpenCVFeatureCount {
  private static final Pattern SPACE = Pattern.compile(" ");

  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: OpenCVFeatureCount <nats url>");
      System.exit(1);
    }
    String nats_url=args[0];

    final NatsClient nc=new NatsClient(nats_url);
    System.out.println("About to connect to nats server at : "+nats_url);

    // Update the cumulative count function
    final Function2<List<Integer>, Optional<Integer>, Optional<Integer>> updateFunction =
        new Function2<List<Integer>, Optional<Integer>, Optional<Integer>>() {
          @Override
          public Optional<Integer> call(List<Integer> values, Optional<Integer> state) {
            Integer newSum = state.or(0);
            for (Integer value : values) {
              newSum += value;
            }
            return Optional.of(newSum);
          }
        };

    //nc.Connect(nats_url);
    //nc.Subscribe("foo");
    //nc.Publish("foo", "Java Nats Client");
    //StreamingExamples.setStreamingLogLevels();

    // Create the context with a 1 second batch size
    SparkConf sparkConf = new SparkConf().setAppName("OpenCVStatefulFeatureCount");
    JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));
    ssc.checkpoint("./ck");

    // Initial RDD input to updateStateByKey
     List<Tuple2<String, Integer>> tuples = Arrays.asList(new Tuple2<String, Integer>("0",0),new Tuple2<String, Integer>("0",0));
    JavaPairRDD<String, Integer> initialRDD = ssc.sc().parallelizePairs(tuples);

    JavaReceiverInputDStream<String> lines = ssc.receiverStream(nc);
    JavaDStream<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
      @Override
      public Iterable<String> call(String x) {
        //System.out.println("Recevied x:"+x);
        String[] ins=SPACE.split(x.replace("\"",""));
        //for (int i=0;i<ins.length ;i++ ) {
        //  ins[i]=ins[i].replace("\"","");
        //}
        return Lists.newArrayList(ins);
      }
    });
    JavaPairDStream<String, Integer> wordsDstream = words.mapToPair(
        new PairFunction<String, String, Integer>() {
          @Override
          public Tuple2<String, Integer> call(String s) {
            return new Tuple2<String, Integer>(s, 1);
          }
        });

    // This will give a Dstream made of state (which is the cumulative count of the words)
    JavaPairDStream<String, Integer> stateDstream = wordsDstream.updateStateByKey(updateFunction,
            new HashPartitioner(ssc.sc().defaultParallelism()), initialRDD);

    stateDstream.print();
    stateDstream.foreachRDD(new Function2<JavaPairRDD<String, Integer>, Time, Void>() {
      @Override
      public Void call(JavaPairRDD<String, Integer> rdd, Time time) throws IOException {
        String counts = "Counts at time " + time + " " + rdd.collect();
        System.out.println(counts);
        nc.Publish("bar",rdd.collect().toString());
        return null;
      }
    });
    ssc.start();
    ssc.awaitTermination();
  }
}

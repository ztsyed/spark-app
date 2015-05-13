# Spark Streaming Application with nats reader/writer

Modified version of JavaNetworkWorkCount example. This app currently reads from `foo` channel and writes on `bar` channel.

### Requirements
1. Java 1.8
2. Maven
3. Spark Streaming 1.3.1
4. nats server
5. some way of subscribing and publishing to nats channels (a simple golang app perhaps. see below)

### Building Spark

Download spark 1.3.1 source and `cd` to the directory and then running the following command
```
build/mvn -Pyarn -Phadoop-2.4 -Dhadoop.version=2.4.0 -DskipTests clean package
```
Details instructions are at https://spark.apache.org/docs/latest/building-spark.html

### Building Spark-App

```
git clone https://github.com/ztsyed/spark-app
cd spark-app
mvn package
```

This should output an `uber-jar` in the `target` folder

### Running
You need to provide a url to the nats server in the format of `nats://ip:port` or `nats://user:pass@ip:port`

Assuming you downloaded and compiled spark at `/spark-1.3.1`
```
/spark-1.3.1/bin/spark-submit --class org.apache.spark.examples.streaming.OpenCVFeatureCount target/spk-app-1.0.jar nats://127.0.0.1:4222
```

### Simple Go application that pub/sub on nats

```
package main

import (
	"fmt"
	"github.com/apcera/nats"
	"math/rand"
	"time"
)

func listen(conn *nats.EncodedConn, c chan string) {
	conn.Subscribe("bar", func(s string) {
		c <- s
	})
}

func main() {
	words := []string{"a", "quick", "brown", "fox", "jumps", "over", "lazy", "dog"}
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	nc, _ := nats.Connect(nats.DefaultURL)
	c, _ := nats.NewEncodedConn(nc, "json")
	defer c.Close()
	buf := make(chan string)
	go listen(c, buf)
	for {
		select {
		case d := <-buf:
			fmt.Println("Received ", d)
		default:
			l := r.Intn(10)
			if l == 0 {
				l = r.Intn(10)
			}
			var str string
			for i := 0; i < l; i++ {
				str += words[r.Intn(len(words))] + " "
			}
			c.Publish("foo", str)
			fmt.Println("Published: ", str)
			time.Sleep(1 * time.Second)
		}
	}

}
```
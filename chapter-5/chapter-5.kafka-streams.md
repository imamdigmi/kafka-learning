# Kafka Streams

## What are Streams?

Kafka is great for messaging between applications, but it also allows you to transform and process data using Kafka Streams.
In this lesson, we will provide an overview of what Kafka streams are. We will also implement a basic Kafka Streams application
using Java.

So far, we have discussed using Kafka for **messageing** (reliably passing data between two applications).

**Kafka Streams** allows us to build applications that process Kafka data in real-time with ease.

A **Kafka Streams application** is an application where both the input and the ouput are stored in Kafka topics.

Kaka Streams is a client library (API) that makes it ease to build these applications.

References:

- [Kafka Streams](https://kafka.apache.org/23/documentation/streams/)

1. Clone the starter project.

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-streams.git
    cd content-ccdak-kafka-streams
    ```

1. Note that the `kafka-client` and `kafka-streams` dependencies have already been added to `build.gradle`.
1. Edit the main class and implement a basic Streams application that simply copies data from the input topic to the output topic.

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/streams/StreamsMain.java
    ```

1. Here is an example of the completed `StreamsMain` class.

    ```java
    package com.linuxacademy.ccdak.streams;

    import java.util.Properties;
    import java.util.concurrent.CountDownLatch;
    import org.apache.kafka.common.serialization.Serdes;
    import org.apache.kafka.streams.KafkaStreams;
    import org.apache.kafka.streams.StreamsBuilder;
    import org.apache.kafka.streams.StreamsConfig;
    import org.apache.kafka.streams.Topology;
    import org.apache.kafka.streams.kstream.KStream;

    public class StreamsMain {
        public static void main(String[] args) {

            // Set up the configuration.
            final Properties props = new Properties();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "inventory-data");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

            // Since the input topic uses Strings for both key and value, set the default Serdes to String.
            props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
            props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

            // Get the source stream.
            final StreamsBuilder builder = new StreamsBuilder();
            final KStream<String, String> source = builder.stream("streams-input-topic");
            source.to("streams-output-topic");

            final Topology topology = builder.build();
            final KafkaStreams streams = new KafkaStreams(topology, props);

            // Print the topology to the console.
            System.out.println(topology.describe());
            final CountDownLatch latch = new CountDownLatch(1);

            // Attach a shutdown handler to catch control-c and terminate the application gracefully.
            Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });

            try {
                streams.start();
                latch.await();
            } catch (final Throwable e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }

            System.exit(0);
        }
    }
    ```

1. Run your Streams application.

    ```sh
    ./gradlew runStreams
    ```

1. In a separate session, use `kafka-console-producer` to publish some data to `streams-input-topic`.

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic streams-input-topic
        --property parse.key=true
        --property key.separator=:
    ```

    and type some data, example:

    ```txt
    hello:world
    test:one
    test:two
    ```

1. In another session, use `kafka-console-producer` to view the data being sent to `streams-output-topic` by your Java application.

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic streams-output-topic
        --property print.key=true
    ```

1. If you have both the producer and consumer running, you should see your Java application pushing data to the output
topic in real time.

References:

- [Kafka Streams](https://kafka.apache.org/23/documentation/streams/)

## Kafka Streams Stateless Transformations

Kafka Streams provides a rich feature set for transforming your data. In this lesson, we will focus on stateless transformations.
We will discuss the difference between stateful and stateless transformations, and we will demonstrate how to use several of
the stateless transformations that are available as part of the Streams API.

Kafka Streams provides a robust set of tools for processing and transforming data. The Kafka cluster itself serves
as the backend for data maangement and storage.

There are two types of data transformations in Kafka Streams:

- **Stateless transformations**
  do not require any additional storage to manage the state.
  - **Branch**
    Split a stream into multiple streams based on a predicate.
  - **Filter**
    Removes messages from the stream based on a condition.
  - **FlatMap**
    Takes input records and turns them into a different set of records.
  - **Foreach**
    Performs an arbitary stateless operation on each record. This is a terminal opertion and stops further processing.
  - **GroupBy/GroupByKey**
    Groups records by their key. This is required to perform stateful transformations.
  - **Map**
    Allows you to read a record and produce a new, modified record.
  - **Merge**
    Merges two streams into one stream.
  - **Peek**
    Silimar to **Foreach**, but does not stop processing.
- **Stateful transformations**
  require a state store to manage the state.

References:

- [Kafka Streams Developer Guide - Stateless Transformations](https://kafka.apache.org/23/documentation/streams/developer-guide/dsl-api.html#stateless-transformations)
- [Branch (deprecated since Kafka 2.8)](https://docs.confluent.io/home/overview.html#branch-org.apache.kafka.streams.kstream.Named-org.apache.kafka.streams.kstream.Predicate...-)
- [Split](https://docs.confluent.io/home/overview.html#split-org.apache.kafka.streams.kstream.Named-)

1. Clone the starter project, if you haven't already done so in a previous lesson.

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-streams.git
    cd content-ccdak-kafka-streams
    ```

1. Edit the `StatelessTransformationsMain` class.

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/streams/StatelessTransformationsMain.java
    ```

1. Implement a Streams application that performs a variety of stateless transformations.

    ```java
    package com.linuxacademy.ccdak.streams;

    import java.util.LinkedList;
    import java.util.List;
    import java.util.Properties;
    import java.util.concurrent.CountDownLatch;
    import org.apache.kafka.common.serialization.Serdes;
    import org.apache.kafka.streams.KafkaStreams;
    import org.apache.kafka.streams.KeyValue;
    import org.apache.kafka.streams.StreamsBuilder;
    import org.apache.kafka.streams.StreamsConfig;
    import org.apache.kafka.streams.Topology;
    import org.apache.kafka.streams.kstream.KStream;

    /*import java.util.Properties;
    import java.util.concurrent.CountDownLatch;*/

    public class StatelessTransformationsMain {
        public static void main(String[] args) {
            // Set up the configuration.
            final Properties props = new Properties();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "stateless-transformations-example");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

            // Since the input topic uses Strings for both key and value, set the default Serdes to String.
            props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
            props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

            // Get the source stream.
            final StreamsBuilder builder = new StreamsBuilder();
            final KStream<String, String> source = builder.stream("stateless-transformations-input-topic");

            // Branch:
            // Split the stream into two streams, one containing all records where the key begins with "a", and t
            KStream<String, String>[] branches = source.branch((key, value) -> key.startsWith("a"), (key, value) -> true);
            KStream<String, String> aKeysStream = branches[0];
            KStream<String, String> othersStream = branches[1];

            // Filter:
            // Remove any records from the "a" stream where the value does not also start with "a".
            aKeysStream = aKeysStream.filter((key, value) -> value.startsWith("a"));

            // FlatMap:
            // For the "a" stream, convert each record into two records, one with an uppercased value and one wit
            aKeysStream = aKeysStream.flatMap((key, value) -> {
                List<KeyValue<String, String> result = new LinkedList<>();
                result.add(KeyValue.pair(key, value.toUpperCase()));
                result.add(KeyValue.pair(key, value.toLowerCase()));
                return result;
            });

            // Foreach
            // known as "terminal operation" that means that we cannot add any additional stream processing
            // after you using foreach, because it does not return anyhing, it return void so we can't add
            // any additional steps after and that includes outputing to an output topic, that's why I commented this line for now.
            // aKeysStream.foreach((key, value) -> System.out.println("key=" + key + ", value=" + value));

            // Map
            // For the "a" stream, modify all records by uppercasing the key.
            aKeysStream = aKeysStream.map((key, value) -> KeyValue.pair(key.toUpperCase(), value));

            // Merge
            // Merge the two streams back together.
            KStream<String, String> mergedStream = aKeysStream.merge(othersStream);

            // Peek
            // Print each record to the console.
            mergedStream.peek((key, value) -> System.out.println("key=" + key + ", value=" + value));

            // Output the transformed data to a topic.
            mergedStream.to("stateless-transformations-output-topic");

            final Topology topology = builder.build();
            final KafkaStreams streams = new KafkaStreams(topology, props);

            // Print the topology to the console.
            System.out.println(topology.describe());
            final CountDownLatch latch = new CountDownLatch(1);

            // Attach a shutdown handler to catch control-c and terminate the application gracefully.
            Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });

            try {
                streams.start();
                latch.await();
            } catch (final Throwable e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }
            System.exit(0);
        }
    }
    ```

1. In a separate session, starts a `kafka-console-producer` to produce data to the input topic.

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic stateless-transformations-input-topic
        --property parse.key=true
        --property key.separator=:
    ```

1. Publish an initial record to automatically create the topic.

    ```txt
    a:a
    ```

1. In the previous session, run your code.

    ```sh
    ./gradlew runStatelessTransformations
    ```

1. In another session, start a `kafka-console-consumer` to view records being published to the output topic, then publish some records and examine how your Streams application modifies them.

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic stateless-transformations-output-topic
        --property print.key=true
    ```

## Kafka Streams Aggregations

Stateless transformations allow you to process records individually, but what if you need some information about multiple
records at the same time? Aggregations allow you to process groups of record that share the same key, and to maintain the
state of your processing in a state store managed in the Kafka cluster. In this lesson, we will discuss what aggregations are and
we will demonstrate how to use three different types of aggregations in a Java application.

Stateless transformations, such as **groupByKey** and **groupBy** can be used to group records that share the same key.

**Aggregations**
Are **stateful transformation** that always operate on these groups of records sharing the same key.

- **Aggregate**
    Generates a new record from a calculation involving the grouped records.
- **Count**
    Counts the number f records for each grouped key.
- **Reduce**
    Combines the grouped records into a single record.

References:

- [Kafka Streams Developer Guide - Aggregating](https://kafka.apache.org/23/documentation/streams/developer-guide/dsl-api.html#aggregating)

1. Clone the starter project, if you haven't already done so in a previous lesson.

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-streams.git
    cd content-ccdak-kafka-streams
    ```

1. Edit the `AggregationsMain` class.

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/streams/AggregationsMain.java
    ```

1. Implement a Streams application that performs a variety of aggregations.

    ```java
    package com.linuxacademy.ccdak.streams;

    import java.util.Properties;
    import java.util.concurrent.CountDownLatch;
    import org.apache.kafka.common.serialization.Serdes;
    import org.apache.kafka.streams.KafkaStreams;
    import org.apache.kafka.streams.StreamsBuilder;
    import org.apache.kafka.streams.StreamsConfig;
    import org.apache.kafka.streams.Topology;
    import org.apache.kafka.streams.kstream.KGroupedStream;
    import org.apache.kafka.streams.kstream.KStream;
    import org.apache.kafka.streams.kstream.KTable;
    import org.apache.kafka.streams.kstream.Materialized;
    import org.apache.kafka.streams.kstream.Produced;

    public class AggregationsMain {
        public static void main(String[] args) {
            // Set up the configuration.
            final Properties props = new Properties();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "aggregations-example");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

            // Since the input topic uses Strings for both key and value, set the default Serdes to String.
            props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
            props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

            // Get the source stream.
            final StreamsBuilder builder = new StreamsBuilder();
            KStream<String, String> source = builder.stream("aggregations-input-topic");

            // Group the source stream by the existing Key.
            // Before we perform any aggregation we do need to group our stream
            KGroupedStream<String, String> groupedStream = source.groupByKey();

            /*
            NOTE:
            All of these aggregation returned a KTable so we need to convert it back to a stream with
            `.toStream()` method before we write it to a topic
            */

            // Aggregate
            // Create an aggregation that totals the length in characters of the value
            // for all records sharing the same key.
            KTable<String, Integer> aggregatedTable = groupedStream.aggregate(
                () -> 0, // Initializer, this initialize an aggregate value
                (aggKey, newValue, aggValue) -> aggValue + newValue.length(), // this is our actual aggregation
                Materialized.with(Serdes.String(), Serdes.Integer()) // used to tell our Kafka streams application how to actually build our data store, because default value of Key and Value are string but in this aggregate we have an integer type of a value
            );

            aggregatedTable.toStream().to(
                "aggregations-output-charactercount-topic",
                Produced.with(Serdes.String(), Serdes.Integer())
            );

            // Count
            // Count the number of records for each key.
            KTable<String, Long> countedTable = groupedStream.count(
                Materialized.with(Serdes.String(), Serdes.Long())
            );
            countedTable.toStream().to(
                "aggregations-output-count-topic",
                Produced.with(Serdes.String(), Serdes.Long())
            );

            // Reduce
            // Combine the values of all records with the same key into a string separated by spaces.
            KTable<String, String> reducedTable = groupedStream.reduce(
                (aggValue, newValue) -> aggValue + " " + newValue
            );
            reducedTable.toStream().to("aggregations-output-reduce-topic"); // We don't need to specify Materialized.with() because the Key-Value are in string type. And we don't use Produce.with() because we don't need a custom SerDe here

            final Topology topology = builder.build();
            final KafkaStreams streams = new KafkaStreams(topology, props);

            // Print the topology to the console.
            System.out.println(topology.describe());
            final CountDownLatch latch = new CountDownLatch(1);

            // Attach a shutdown handler to catch control-c and terminate the application gracefully.
            Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });

            try {
                streams.start();
                latch.await();
            } catch (final Throwable e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }

            System.exit(0);
        }
    }
    ```

1. In a separate session, start a `kafka-console-producer` to produce data to the input topic.

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic aggregations-input-topic
        --property parse.key=true
        --property key.separator=:
    ```

1. Publish an initial record to automatically create the topic.

    ```txt
    a:a
    ```

1. In the previous session, run your code

    ```sh
    ./gradlew runAggregations
    ```

1. Open three more sessions. In each one, start a `kafka-console-consumer` to view records being published to the three output
topics, then publish some records to the input topic and examine how your streams application modifies them.

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic aggregations-output-charactercount-topic
        --property print.key=true
        --property value.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer
    ```

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic aggregations-output-count-topic
        --property print.key=true
        --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
    ```

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic aggregations-output-reduce-topic
        --property print.key=true
    ```

## Kafka Streams Joins

Joins are used to combine streams into one new stream.

Kafka provides a variety of tools for manipulating data from individual streams. But what if you need to work with
corresponding data records from multiple topics? Kafka joins allow you to combine streams, joining individual records from
both streams based upon their shared keys (much like you would do with foreign keys in a relational database). In this
lesson, we will discuss some of the benefits and limitations of joins. We will also demonstrate three types of joins in the
context of a Kafka Streams application.

**Co-Partitioning**
When joining streams, the data must be co-partitioned:

- Same number of partitions for input topics
- Same partitioning strategies for producers

You can avoid the need for co-partitioning by using a **GlobalKTable**. With GlobalKTables, all instances of your
streams application will populate the local table with data from **all partitions**.

> The Kafka Streams Join are similar with the join concept in the relational database.

Different kind of joins:

- **Inner Join**
    The new stream will contain only records that have a match in both joined streams.
- **Left Join**
    The new stream will contain all records from the first stream, but only matching records from the joined stream.
- **Outer Join**
    The new stream will contain all records from both streams.

References:

- [Joining](https://kafka.apache.org/23/documentation/streams/developer-guide/dsl-api.html#joining)

1. Clone the starter project, if you haven't already done so in a previous lesson.

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-streams.git
    cd content-ccdak-kafka-streams
    ```

1. Edit the `JoinsMain` class.

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/streams/JoinsMain.java
    ```

1. Implement a Streams application that performs a variety of joins.

    ```java
    package com.linuxacademy.ccdak.streams;

    import java.time.Duration;
    import java.util.Properties;
    import java.util.concurrent.CountDownLatch;
    import org.apache.kafka.common.serialization.Serdes;
    import org.apache.kafka.streams.KafkaStreams;
    import org.apache.kafka.streams.StreamsBuilder;
    import org.apache.kafka.streams.StreamsConfig;
    import org.apache.kafka.streams.Topology;
    import org.apache.kafka.streams.kstream.JoinWindows;
    import org.apache.kafka.streams.kstream.KStream;

    public class JoinsMain {
        public static void main(String[] args) {

            // Set up the configuration.
            final Properties props = new Properties();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "joins-example");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

            // Since the input topic uses Strings for both key and value, set the default Serdes to String.
            props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
            props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

            // Get the source stream.
            final StreamsBuilder builder = new StreamsBuilder();
            KStream<String, String> left = builder.stream("joins-input-topic-left");
            KStream<String, String> right = builder.stream("joins-input-topic-right");

            // Perform an inner join.
            KStream<String, String> innerJoined = left.join(
                right,
                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,  // this is called value joiner
                JoinWindows.of(Duration.ofMinutes(5))
            );
            innerJoined.to("inner-join-output-topic");

            // Perform a left join.
            KStream<String, String> leftJoined = left.leftJoin(
                right,
                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
                JoinWindows.of(Duration.ofMinutes(5))
            );
            leftJoined.to("left-join-output-topic");

            // Perform an outer join.
            KStream<String, String> outerJoined = left.outerJoin(
                right,
                (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
                JoinWindows.of(Duration.ofMinutes(5))
            );
            outerJoined.to("outer-join-output-topic");

            final Topology topology = builder.build();
            final KafkaStreams streams = new KafkaStreams(topology, props);

            // Print the topology to the console.
            System.out.println(topology.describe());
            final CountDownLatch latch = new CountDownLatch(1);

            // Attach a shutdown handler to catch control-c and terminate the application gracefully.
            Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });

            try {
                streams.start();
                latch.await();
            } catch (final Throwable e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }

            System.exit(0);
        }
    }
    ```

1. Open two separate sessions. In each one, start a `kafka-console-producer` to produce data to one of the input topics.

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic joins-input-topic-left
        --property parse.key=true
    ```

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic joins-input-topic-right
        --property parse.key=true
    ```

1. Publish an initial record to each topic to automatically create both topics.

    ```txt
    a:a
    ```

    ```txt
    b:b
    ```

1. In the previous session, run your code.

    ```sh
    ./gradlew runJoins
    ```

1. Open three more sessions. In each one, start a `kafka-console-consumer` to view records being published to the three
output topics, then publish some records to the input topics and examine how your Streams application modifies them.

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic inner-join-output-topic
        --property print.key=true
    ```

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic left-join-output-topic
        --property print.key=true
    ```

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic outer-join-output-topic
        --property print.key=true
    ```

## Kafka Streams Windowing

There is a lot you can do with aggregations and joins, but Kafka also provides the ability to perform aggregations and joins
within the context of specific time buckets known as windows. In this lesson, we will discuss what windowing is, and we will
demonstrate the use of windowing in the context of a basic aggregation.

- **Windows**
    Are similar to groups in that they deal with a set of records with the same key. However, windows further
    subdivide groups into "time buckets".
- **Tumbling Time Windows**
    Windows are based on time periods that never overlap or have gaps between them.
- **Hoping Time Windows**
    Time-based, but can have overlaps or gaps between windows.
- **Sliding Time Windows**
    These windows are dynamically based on the timestamps of records rather than a fixed point in time.
    They are only used in joins.
- **Session Windows**
    Creates windows based on periods of activity. A group of records around the same timestamp will form a session window,
    whereas a period of "idle time" with no records in the group will not have a window.
- **Late-Arriving Records**
    In real-world scenarios, it is always possible to receive out-of-order data.

    When records fall into a time window reveived after the end of that window's grace period, they become known as **late-arriving records**.

    You can specify a **retention period** for a window. Kafka Streams will retain old window buckets during this
    period so that late-arriving records can still be processed.

    Any records that arrive after the retention period has expired will not be processed.

References:

- [Kafka Streams Developer Guide - Windowing](https://kafka.apache.org/23/documentation/streams/developer-guide/dsl-api.html#windowing)

1. Clone the starter project, if you haven't already done so in a previous lesson.

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-streams.git
    cd content-ccdak-kafka-streams
    ```

1. Edit the `WindowingMain` class.

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/streams/WindowingMain.java
    ```

1. Implement a Streams application that demonstrates windowing.

    ```java
    package com.linuxacademy.ccdak.streams;

    import java.time.Duration;
    import java.util.Properties;
    import java.util.concurrent.CountDownLatch;
    import org.apache.kafka.common.serialization.Serdes;
    import org.apache.kafka.streams.KafkaStreams;
    import org.apache.kafka.streams.StreamsBuilder;
    import org.apache.kafka.streams.StreamsConfig;
    import org.apache.kafka.streams.Topology;
    import org.apache.kafka.streams.kstream.KGroupedStream;
    import org.apache.kafka.streams.kstream.KStream;
    import org.apache.kafka.streams.kstream.KTable;
    import org.apache.kafka.streams.kstream.Produced;
    import org.apache.kafka.streams.kstream.TimeWindowedKStream;
    import org.apache.kafka.streams.kstream.TimeWindows;
    import org.apache.kafka.streams.kstream.Windowed;
    import org.apache.kafka.streams.kstream.WindowedSerdes;

    public class WindowingMain {
        public static void main(String[] args) {

            // Set up the configuration.
            final Properties props = new Properties();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "windowing-example");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

            // Since the input topic uses Strings for both key and value, set the default Serdes to String.
            props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
            props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

            // Get the source stream.
            final StreamsBuilder builder = new StreamsBuilder();
            KStream<String, String> source = builder.stream("windowing-input-topic");
            KGroupedStream<String, String> groupedStream = source.groupByKey();

            // Apply windowing to the stream with tumbling time windows of 10 seconds.
            TimeWindowedKStream<String, String> windowedStream = groupedStream.windowedBy(
                /*
                Specifying duration is also known as window size,
                below example means that each one of our windows is going to be ten seconds long, and this
                means we're using tumbling time windows
                */
                TimeWindows.of(Duration.ofSeconds(10))

                /*
                we can also specify `.advancedBy(Duration.ofSeconds(10))`
                example:
                TimeWindows.of(Duration.ofSeconds(10)).advancedBy(Duration.ofSeconds(10))

                That define what's called as advanced interval or hop. This determin of how we opened up
                a new window, means that we are going to start a new window every ten seconds.

                But if we set the duration to 12 seconds that mean we have two seconds gap and this means we
                are using a hoping window setup, because we've created a gaps in between our windows. And if we
                set the duration to 10 seconds than that means no gap or overlaps and that is means we're
                using tumbling window setup.

                So you can control wheter we using tumbling time windows or hopping time windows simply by
                adjusting these two settings
                */
            );

            // Combine the values of all records with the same key into a string separated by spaces, using 10-se
            KTable<Windowed<String>, String> reducedTable = windowedStream.reduce(
                (aggValue, newValue) -> aggValue + " " + newValue
            );
            reducedTable.toStream().to(
                "windowing-output-topic",
                /*
                Because we are using Windowed Type so we need to convert the type and using custom serde
                */
                Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String.class), Serdes.String())
            );

            final Topology topology = builder.build();
            final KafkaStreams streams = new KafkaStreams(topology, props);

            // Print the topology to the console.
            System.out.println(topology.describe());
            final CountDownLatch latch = new CountDownLatch(1);

            // Attach a shutdown handler to catch control-c and terminate the application gracefully.
            Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });

            try {
                streams.start();
                latch.await();
            } catch (final Throwable e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }

            System.exit(0);
        }
    }
    ```

1. Open a separate session. Start a `kafka-console-producer` to produce data to the input topic.

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic windowing-input-topic
        --property parse.key=true
        --property key-separator=:
    ```

1. Publish an initial record to automatically create the topic.

    ```txt
    a:a
    ```

1. In the previous session, run your code.

    ```sh
    ./gradlew runWindowing
    ```

1. Open an additional session. Start a `kafka-console-consumer` to view records being published to the output topic, then
publish some records to the input topic and examine how your Streams application modifies them.

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic windowing-output-topic
        --property print.key=true
    ```

    > **Note**: You'll notice our key look a litle strange character like � that's because the `kafka-console-consumer` doesn't have a deserializer setup that able to handle these windowed keys, and that's because the key actually contain the time windowe data in adition to the actual string value of the key, but we can see the actual key name

## Streams vs. Tables

Kafka Streams models data in two primary ways: **streams** and **tables**.

**Streams**
Each records is a self-contained piece of data in an unbounded set of data. New records do not replace an existing
piece of data with a new value.

**Tables**
Records represent a current state that can be overwritten/updated.

Here are some example use cases:
**Stream**:

- Credit card transactions in real time.
- A real-time log of attendees checking in to a conference.
- A log of customer purchases which represent the removal of items from a store's inventory.

**Tables**:

- A user's current available credit card balance.
- A list of conference attendee names with a value indicating whether or not they have checked in.
- a set of data containing the quantity of each item in a store's inventory.

References:

- [Duality of Streams and Tables](https://docs.confluent.io/platform/current/streams/concepts.html#duality-of-streams-and-tables)

## [Hands-On] Working with Stream Processing in Kafka

In this hands-on lab, we will have the opportunity to work with Kafka streams by building a Java application capable of transforming data about individual purchases into a running total of each item that was bought.

Your supermarket company is working toward using Kafka to automate some aspects of inventory management.
Currently, they are trying to use Kafka to keep track of purchases so that inventory systems can be updated
as items are bought. So far, the company has created a Kafka topic where they are publishing information
about the types and quantities of items being purchased.

When a customer makes a purchase, a record is published to the `inventory_purchases` topic for each item type
(i.e., "apples"). These records have the item type as the key and the quantity purchased in the transaction
as the value. An example record would look like this, indicating that a customer bought five apples:

```txt
apples:5
```

Your task is to build a Kafka streams application that will take the data about individual item purchases
and output a running total purchase quantity for each item type. The output topic is called `total_purchases`.
So, for example, with the following input from `inventory_purchases`:

```txt
apples:5
oranges:2
apples:3
```

Your streams application should output the following to `total_purchases`:

```txt
apples:8
oranges:2
```

Be sure to output the total quantity as an `Integer`. Note that the input topic has the item quantity
serialized as a `String`, so you will need to work around this using type conversion.

To get started, use the starter project located at [https://github.com/linuxacademy/content-ccdak-kafka-streams-lab](https://github.com/linuxacademy/content-ccdak-kafka-streams-lab). This GitHub project also contains an `end-state` branch with an example solution for the lab.

You should be able to perform all work on the `Broker 1` server.

1. Clone the starter project from GitHub:

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-streams-lab.git
    ```

1. Change directories using:

    ```sh
    cd content-ccdak-kafka-streams-lab/
    ```

    > Note: We can use ls to see into our directory further.

1. Perform a test to ensure that the code can compile and run:

    ```sh
    ./gradlew run
    ```

    The output should contain the message printed by the Main class: Hello, world!

1. We can `clear` our screen.
1. Access the build.gradle file that we will edit:

    ```sh
    vi build.gradle
    ```

1. Add the following to the dependencies `{...}` section of the file, above testImplementation:

    ```gradle
    implementation 'org.apache.kafka:kafka-streams:2.2.1'
    implementation 'org.apache.kafka:kafka-clients:2.2.1'
    ```

1. Implement our streams logic in `Main.java`:

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/streams/Main.java
    ```

    > Note: Remember to check the code comments for some helpful info, or the solution video for a more thorough explanation.

1. Here is an example of the `Main.java` code:

    ```java
    package com.linuxacademy.ccdak.streams;

    import java.util.Properties;
    import java.util.concurrent.CountDownLatch;
    import org.apache.kafka.common.serialization.Serdes;
    import org.apache.kafka.streams.KafkaStreams;
    import org.apache.kafka.streams.StreamsBuilder;
    import org.apache.kafka.streams.StreamsConfig;
    import org.apache.kafka.streams.Topology;
    import org.apache.kafka.streams.kstream.Grouped;
    import org.apache.kafka.streams.kstream.KStream;
    import org.apache.kafka.streams.kstream.KTable;
    import org.apache.kafka.streams.kstream.Produced;

    public class Main {

        public static void main(String[] args) {
            // Set up the configuration.
            final Properties props = new Properties();
            props.put(StreamsConfig.APPLICATION_ID_CONFIG, "inventory-data");
            props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
            // Since the input topic uses Strings for both key and value, set the default Serdes to String.
            props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
            props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

            // Get the source stream.
            final StreamsBuilder builder = new StreamsBuilder();
            final KStream<String, String> source = builder.stream("inventory_purchases");

            // Convert the value from String to Integer. If the value is not a properly-formatted number, print a message and set it to 0.
            final KStream<String, Integer> integerValuesSource = source.mapValues(value -> {
                try {
                    return Integer.valueOf(value);
                } catch (NumberFormatException e) {
                    System.out.println("Unable to convert to Integer: \"" + value + "\"");
                    return 0;
                }
            });

            // Group by the key and reduce to provide a total quantity for each key.
            final KTable<String, Integer> productCounts = integerValuesSource
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Integer()))
                .reduce((total, newQuantity) -> total + newQuantity);

            // Output to the output topic.
            productCounts
                .toStream()
                .to("total_purchases", Produced.with(Serdes.String(), Serdes.Integer()));

            final Topology topology = builder.build();
            final KafkaStreams streams = new KafkaStreams(topology, props);
            // Print the topology to the console.
            System.out.println(topology.describe());
            final CountDownLatch latch = new CountDownLatch(1);

            // Attach a shutdown handler to catch control-c and terminate the application gracefully.
            Runtime.getRuntime().addShutdownHook(new Thread("streams-wordcount-shutdown-hook") {
                @Override
                public void run() {
                    streams.close();
                    latch.countDown();
                }
            });

            try {
                streams.start();
                latch.await();
            } catch (final Throwable e) {
                System.out.println(e.getMessage());
                System.exit(1);
            }

            System.exit(0);
        }

    }

    ```

1. Run the code:

    ```sh
    ./gradlew run
    ```

1. We can check the output of the application by consuming from the output topic:

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic total_purchases
        --from-beginning
        --property print.key=true
        --property value.deserializer=org.apache.kafka.common.serialization.IntegerDeserializer
    ```

1. If we want to visually compare the output with the input, in a new terminal window, we can consume from the input topic with:

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic inventory_purchases
        --property print.key=true
    ```

# Working with Kafka in Java

## Java Producers

Producers write data to Kafka Topics.

Kafka's Java Producer API simplifies the process of building your own custom producers. In this lesson, we will take a deeper
dive into the Producer API. We will build a producer that demonstrates a few of the more advanced features that the Producer
API provides.

Yuo can create your own producer in Java using the **Producer API**.

Let's create a producer to:

- Count from 0 to 99 and publish the counts as records to a topic.
- Print the record metadata to te console using a callback.

You can configure a producer with `acks=all`. This will cause the producer to receive an acknowledgement for
record only after all in-sync replicas have acknowledged the record.

References

- [Producer API](https://kafka.apache.org/documentation/#producerapi)
- [Class Producer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/producer/KafkaProducer.html)

1. Create a topic to use for testing.

    ```sh
    kafka-topics
        --bootstrap-server localhost:9092
        --create
        --topic test_count
        --partitions 2
        --replication-factor 1
    ```

1. Clone the starter project.

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-producers-and-consumers.git
    ```

1. Edit the `ProducerMain` class.

    ```sh
    cd content-ccdak-kafka-producers-and-consumers
    vi src/main/java/com/linuxacademy/ccdak/clients/ProducerMain.java
    ```

1. Implement a producer.

    ```java
    package com.linuxacademy.ccdak.clients;

    import java.util.Properties;
    import org.apache.kafka.clients.producer.KafkaProducer;
    import org.apache.kafka.clients.producer.Producer;
    import org.apache.kafka.clients.producer.ProducerRecord;
    import org.apache.kafka.clients.producer.RecordMetadata;

    public class ProducerMain {

        public static void main(String[] args) {
            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:9092");
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            props.put("acks", "all");

            Producer<String, String> producer = new KafkaProducer<>(props);

            for (int i = 0; i < 100; i++) {
                int partition = 0;
                if (i > 49) {
                    partition = 1;
                }
                ProducerRecord record = new ProducerRecord<>("test_count", partition, "count", Integer.toString(i));
                producer.send(record, (RecordMetadata metadata, Exception e) -> {
                    if (e != null) {
                        System.out.println("Error publishing message: " + e.getMessage());
                    } else {
                        System.out.println("Published message: key=" + record.key() +
                                ", value=" + record.value() +
                                ", topic=" + metadata.topic() +
                                ", partition=" + metadata.partition() +
                                ", offset=" + metadata.offset());
                    }
                });
            }

            producer.close();
        }

    }

    ```

1. Run the producer code

    ```sh
    ./gradlew runProducer
    ```

1. Verify that the expected data appears in the output topic.

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic test_count
        --property print.key=true
        --from-beginning
    ```

## Java Consumers

Kafka consumers allow you to read and respond to Kafka data. The Java Consumer API makes the process of building these
consumers easy. In this lesson, we will build a basic consumer using the Consumer API. We will also demonstrate a few of the
more advanced features that you can use to get the most out of your consumers.

Consumers read data from Kafka topics.

You can create your own consumer in Java using the **Consumer API**.

Let's create a consumer to:

- Consume data from two topics.
- Print the key and value to the console, along with some metadata such as the message partition and offset.

References:

- [Consumer API](https://kafka.apache.org/documentation/#consumerapi)
- [Kafka Consumer](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html)

1. If you have not already done so in a previous lesson, clone the starter project.

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-producers-and-consumers.git
    ```

1. Create two test topics to consume.

    ```sh
    kafka-topics
        --bootstrap-server localhost:9092
        --create
        --topic test_topic1
        --partitions 2
        --replication-factor 1

    kafka-topics
        --bootstrap-server localhost:9092
        --create
        --topic test_topic2
        --partitions 2
        --replication-factor 1
    ```

1. Edit the `ConsumerMain` class.

    ```sh
    cd /home/cloud_user/content-ccdak-kafka-producers-and-consumers
    vi src/main/java/com/linuxacademy/ccdak/clients/ConsumerMain.java
    ```

    ```java
    package com.linuxacademy.ccdak.clients;

    import java.time.Duration;
    import java.util.Arrays;
    import java.util.Properties;
    import org.apache.kafka.clients.consumer.ConsumerRecord;
    import org.apache.kafka.clients.consumer.ConsumerRecords;
    import org.apache.kafka.clients.consumer.KafkaConsumer;

    public class ConsumerMain {

        public static void main(String[] args) {
            Properties props = new Properties();
            props.setProperty("bootstrap.servers", "localhost:9092");
            props.setProperty("group.id", "group1");
            props.setProperty("enable.auto.commit", "false");
            props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList("test_topic1", "test_topic2"));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("key=" + record.key() + ", value=" + record.value() + ", topic=" + record.topic() + ", partition=" + record.partition() + ", offset=" + record.offset());
                }

                // Because we disable autocommit then we need to manually commit processing the data to the topics.
                // This is very useful when we need a fully control on how we commit the data based on a certain condition
                // maybe when it raise an exception we don't want to commit it otherwise we commit the data.
                consumer.commitSync();
            }
        }

    }

    ```

1. Execute the consumer code.

    ```sh
    ./gradlew runConsumer
    ```

1. Open a new shell, and run a console producer.

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic test_topic1
    ```

1. Open another new shell, and run a second console produce

    ```sh
    kafka-console-producer
        --broker-list localhost:9092
        --topic test_topic2
    ```

1. Publish some data from both console producers and watch how the consumer reacts.

## [Hands-On] Building a Kafka Producer in Java

Your supermarket company is using Kafka to manage data related to inventory. They have some files containing
data about transactions and want you to build a producer that is capable of reading these files and
publishing the data to Kafka.

There is a sample transaction log file containing an example of some of this data. The file contains data
in the format `<product>:<quantity>`, for example: `apples:5`. Each line in the file represents a new transaction.
Build a producer that reads each line in the file and publishes a record to the `inventory_purchases` topic.
Use the product name as the key and the quantity as the value.

The company also wants to track purchases of `apples` in a separate topic
(in addition to the `inventory_purchases` topic). So, for records that have a key of `apples`, publish them
both to the `inventory_purchases` and the `apple_purchases` topic.

Finally, to maintain maximum data integrity set `acks` to `all` for your producer.

There is a starter project located in GitHub which you can use to implement your producer:
[https://github.com/linuxacademy/content-ccdak-kafka-producer-lab.git](https://github.com/linuxacademy/content-ccdak-kafka-producer-lab.git). Clone this project and implement the producer in its `Main` class.
You can execute the main class from the project directory with the `./gradlew run` command.

The sample transaction log file can be found inside the starter project at `src/main/resources/sample_transaction_log.txt`.

If you get stuck, feel free to check out the solution video, or the detailed instructions under each objective. Good luck!

1. Clone the starter project into the `home` directory:

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-producer-lab.git
    ```

1. View the creation of the `content-ccdak-kafka-producer-lab` directory:

    ```sh
    ls
    ```

1. Run the code to ensure it works before modifying it:

    ```sh
    cd content-ccdak-kafka-producer-lab/
    ./gradlew run
    ```

    > Note: We should see a `Hello, world!` message in the output.

1. Edit the main class:

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/producer/Main.java
    ```

1. Implement the producer according to the provided specification:

    ```java
    package com.linuxacademy.ccdak.producer;

    import java.io.BufferedReader;
    import java.io.File;
    import java.io.FileReader;
    import java.io.IOException;
    import java.util.Properties;
    import org.apache.kafka.clients.producer.KafkaProducer;
    import org.apache.kafka.clients.producer.Producer;
    import org.apache.kafka.clients.producer.ProducerRecord;

    public class Main {

        public static void main(String[] args) {
            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:9092");
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            props.put("acks", "all");

            Producer<String, String> producer = new KafkaProducer<>(props);

            try {
                File file = new File(Main.class.getClassLoader().getResource("sample_transaction_log.txt").getFile());
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] lineArray = line.split(":");
                    String key = lineArray[0];
                    String value = lineArray[1];
                    producer.send(new ProducerRecord<>("inventory_purchases", key, value));
                    if (key.equals("apples")) {
                        producer.send(new ProducerRecord<>("apple_purchases", key, value));
                    }
                }
                br.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            producer.close();
        }

    }
    ```

1. Save and exit.
1. Execute the program:

    ```sh
    ./gradlew run
    ```

    Note: We should see a `BUILD SUCCESSFUL` message.

1. Consume the records from the `inventory_purchases` topic and verify that we can see the new records created by the producer:

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic inventory_purchases
        --property print.key=true
        --from-beginning
    ```

1. Consume the records from the `apple_purchases` topic to verify that we can see the new records created by the producer:

    ```sh
    kafka-console-consumer
        --bootstrap-server localhost:9092
        --topic apple_purchases
        --property print.key=true
        --from-beginning
    ```

## [Hands-On] Building a Kafka Consumer in Java

Your supermarket company is using Kafka to process inventory data. They have a topic called
`inventory_purchases` which is receiving data about the items being purchased and the quantity.
However, there is still a legacy system which must ingest this data in the form of a data file.

You have been asked to create a consumer that will read the data from the topic and output to a data file.
Each record should be on its own line, and should have the following format:

```txt
key=<key>, value=<value>, topic=<topic>, partition=<partition>, offset=<offset>
```

There is a starter project located in GitHub which you can use to implement your producer: [https://github.com/linuxacademy/content-ccdak-kafka-consumer-lab.git](https://github.com/linuxacademy/content-ccdak-kafka-consumer-lab.git). Clone this project and implement the consumer in its Main class. You can execute the main class from the project directory with the `./gradlew run` command.

The output data should go into the following file: `/home/cloud_user/output/output.dat`.

If you get stuck, feel free to check out the solution video, or the detailed instructions under each objective. Good luck!

1. Clone the starter project into the home directory:

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-consumer-lab.git
    ```

1. View the creation of the content-ccdak-kafka-consumer-lab output directory with:

    ```sh
    ls
    ```

1. Run the code to ensure it works before modifying it:

    ```sh
    cd content-ccdak-kafka-consumer-lab/
    ./gradlew run
    ```

    > Note: We should see a Hello, World! message in the output.

1. Edit the main class:

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/consumer/Main.java
    ```

1. Implement the consumer according to the provided specification:

    ```java
    package com.linuxacademy.ccdak.consumer;

    import java.io.BufferedWriter;
    import java.io.FileWriter;
    import java.io.IOException;
    import java.time.Duration;
    import java.util.Arrays;
    import java.util.Properties;
    import org.apache.kafka.clients.consumer.ConsumerRecord;
    import org.apache.kafka.clients.consumer.ConsumerRecords;
    import org.apache.kafka.clients.consumer.KafkaConsumer;

    public class Main {

        public static void main(String[] args) {
            Properties props = new Properties();
            props.setProperty("bootstrap.servers", "localhost:9092");
            props.setProperty("group.id", "group1");
            props.setProperty("enable.auto.commit", "true");
            props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList("inventory_purchases"));
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter("/home/cloud_user/output/output.dat", true));
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        String recordString = "key=" + record.key() + ", value=" + record.value() + ", topic=" + record.topic() + ", partition=" + record.partition() + ", offset=" + record.offset();
                        System.out.println(recordString);
                        writer.write(recordString + "\n");
                    }
                    consumer.commitSync();
                    writer.flush();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    ```

1. Save and exit.
1. Execute the program:

    ```sh
    ./gradlew run
    ```

    > Note: We should see an `EXECUTING` message as the real-time records appear.

1. Verify that data is appearing in the output file:

    ```sh
    cat /home/cloud_user/output/output.dat
    ```

    ```txt
    key=apple, value=5, topic=inventory_purchases, partition=0, offset=134
    key=orange, value=0, topic=inventory_purchases, partition=0, offset=135
    key=lemon, value=9, topic=inventory_purchases, partition=0, offset=136
    key=orange, value=6, topic=inventory_purchases, partition=0, offset=137
    key=pear, value=9, topic=inventory_purchases, partition=0, offset=138
    key=apple, value=2, topic=inventory_purchases, partition=0, offset=139
    key=pear, value=6, topic=inventory_purchases, partition=0, offset=140
    key=lemon, value=4, topic=inventory_purchases, partition=0, offset=141
    key=orange, value=5, topic=inventory_purchases, partition=0, offset=142
    key=lemon, value=0, topic=inventory_purchases, partition=0, offset=143
    key=lemon, value=4, topic=inventory_purchases, partition=0, offset=144
    key=orange, value=6, topic=inventory_purchases, partition=0, offset=145
    key=lemon, value=8, topic=inventory_purchases, partition=0, offset=146
    ```

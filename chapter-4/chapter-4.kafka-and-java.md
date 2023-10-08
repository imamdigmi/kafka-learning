# Kafka and Java

## The Kafka Java APIs

Kafka provides a series of **Application Programming Interface (APIs)**. These make it easier to write application
that use Kafka.

Kafka maintains a set of client libraries for **Java**, although there are open-source projects providing similar support
for a variety of other languages.

Include there client libraries in your application to easily interact with Kafka!

- **Producer API**: Allows you to build producers that publish messages to Kafka.
- **Consumer API**: Allows you to build consumers that read Kafka messages.
- **Streams API**: Allows you to read form input topics, transform data, and output it to output topics.
- **Connect API**: Allows you to build custom connectors, which pull from or push to specific external system.
- **AdminClient API**: Allows you to manage and inspect higher-level objects like topics and brokers.

Reference:

- [Kafka APIs](https://kafka.apache.org/documentation/#api)

1. Clone the starter project:

    ```sh
    git clone https://github.com/linuxacademy/content-ccdak-kafka-java-connect.git
    ```

1. Add the necessary dependency to `build.gradle`:

    ```sh
    vi build.gradle
    ```

1. Add the `kafka-client` dependency in the `dependencies {...}` block:

    ```gradle
    dependencies {
        implementation 'org.apache.kafka:kafka-clients:2.2.1'
        testImplementation 'junit:junit:4.12'
    }
    ```

1. Edit the `main class`, and implement a simple producer:

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/kafkaJavaConnect/Main.java
    ```

    ```java
    package com.linuxacademy.ccdak.kafkaJavaConnect;

    import org.apache.kafka.clients.producer.*;
    import java.util.Properties;

    public class Main {
        public static void main(String[] args) {
            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:9092");
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

            Producer<String, String> producer = new KafkaProducer<>(props);
            for (int i = 0; i < 100; i++) {
                producer.send(new ProducerRecord<String, String>("count-topic", "count", Integer.toString(i)));
            }
            producer.close();
        }
    }

    ```

1. Run your code to produce some data to `count-topic`:

    ```sh
    ./gradlew run
    ```

1. Read from `count-topic` to verify that the data from your producer published to the topic successfully:

    ```sh
    kafka-console-consumer --bootstrap-server localhost:9092 --topic count-topic --from-beginning
    ```

## [Hands-On] Connecting to Kafka Programmatically in Java

In this hands-on lab, we will build a simple Java program that consumes data from a Kafka topic.

Your supermarket company is using Kafka to track and process purchase data to dynamically keep track of inventory changes. They have a topic called `inventory purchases`, data about the type, plus the number of items purchased that get published to this topic.

You have been asked to begin the process of writing a Java application that can consume this data. As an initial step, your current task is to write a simple Java program that can consume messages from the Kafka topic and print it to the console.

You can find a starter java project here: [](https://github.com/linuxacademy/content-ccdak-kafka-simple-consumer) Clone the starter project into your home folder and implement the Kafka consumer inside the `Main` class.

This project includes a basic project framework and a Gradle build to allow you to compile run the code easily. From within the leading project directory, you can run the code in the `Main` class with the command `./gradlew run`.

You can find some examples of Kafka consumer code in the [Kafka Consumer API Javadocs](https://kafka.apache.org/23/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html). The GitHub starter project also includes an example solution in the [end-state branch](https://github.com/linuxacademy/content-ccdak-kafka-simple-consumer/tree/end-state).

Do all of your work on the `Broker 1` server. You can access the Kafka cluster from that server at `localhost:9092`.

If you get stuck, feel free to check out the solution video, or the detailed instructions under each objective. Good luck!

### Clone the Starter Project from GitHub and Perform a Test Run

1. Clone the starter project from GitHub:

    ```sh
    cd ~/
    git clone https://github.com/linuxacademy/content-ccdak-kafka-simple-consumer.git
    ```

    > **Note**: We can use `ls` to view the folder named after the repository.

1. Perform a test to make sure the code is able to compile and run:

    ```sh
    cd content-ccdak-kafka-simple-consumer/
    ./gradlew run
    ```

    The output should contain the message printed by the `main class`: `Hello, world!`.

### Implement a Consumer in the Main Class and Run It

1. Add the Kafka Client Libraries as a project dependency in `build.gradle` with:

    ```sh
    vi build.gradle
    ```

1. In the dependencies `{ ... }` block, add the following line:

    ```gradle
    implementation 'org.apache.kafka:kafka-clients:2.2.1'
    ```

1. Edit the `Main` class:

    ```sh
    vi src/main/java/com/linuxacademy/ccdak/kafkaSimpleConsumer/Main.java
    ```

1. Implement a basic consumer that consumes messages from the topic and prints them to the screen, save and exit.:

    ```java
    package com.linuxacademy.ccdak.kafkaSimpleConsumer;

    import org.apache.kafka.clients.consumer.*;
    import java.util.Properties;
    import java.util.Arrays;
    import java.time.Duration;

    public class Main {
        public static void main(String[] args) {
            Properties props = new Properties();
            props.setProperty("bootstrap.servers", "localhost:9092");
            props.setProperty("group.id", "my-group");
            props.setProperty("enable.auto.commit", "true");
            props.setProperty("auto.commit.interval.ms", "1000");
            props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
            consumer.subscribe(Arrays.asList("inventory_purchases"));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
                }
            }
        }
    }
    ```

1. Run the code:

    ```sh
    ./gradlew run
    ```

The program should print a series of messages from the Kafka topic containing information about item purchases. We should see an `offset`, `key`, `value`, `id`, `product`, and `quantity` listed for each record in the output.

```txt
offset = 345, key = null, value = id: 346, product: pear, quantity: 4
offset = 346, key = null, value = id: 347, product: pear, quantity: 0
offset = 347, key = null, value = id: 348, product: apple, quantity: 0
offset = 348, key = null, value = id: 349, product: orange, quantity: 9
offset = 349, key = null, value = id: 350, product: orange, quantity: 8
offset = 350, key = null, value = id: 351, product: pear, quantity: 5
offset = 351, key = null, value = id: 352, product: lemon, quantity: 2
offset = 352, key = null, value = id: 353, product: pear, quantity: 2
offset = 353, key = null, value = id: 354, product: lemon, quantity: 5
...
<=========----> 75% EXECUTING [3m 50s]
> :run
```

> **Note**: Use the Ctrl + C keyboard shortcut to stop printing messages.

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

package com.linuxacademy.ccdak.client.config;

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

        // HERE:
        // we add moer configs
        props.put("acks", "all");
        props.put("buffer.memory", "12582912");
        props.put("connections.max.idle.ms", "300000");

        Producer<String, String> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("inventory_purchases", "apples", "1"));
        producer.send(new ProducerRecord<>("inventory_purchases", "apples", "3"));
        producer.send(new ProducerRecord<>("inventory_purchases", "oranges", "12"));
        producer.send(new ProducerRecord<>("inventory_purchases", "bananas", "25"));
        producer.send(new ProducerRecord<>("inventory_purchases", "pears", "15"));
        producer.send(new ProducerRecord<>("inventory_purchases", "apples", "6"));
        producer.send(new ProducerRecord<>("inventory_purchases", "pears", "7"));
        producer.send(new ProducerRecord<>("inventory_purchases", "oranges", "1"));
        producer.send(new ProducerRecord<>("inventory_purchases", "grapes", "56"));
        producer.send(new ProducerRecord<>("inventory_purchases", "oranges", "11"));
        producer.close();
    }

}

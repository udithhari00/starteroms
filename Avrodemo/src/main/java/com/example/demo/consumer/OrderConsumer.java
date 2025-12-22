package com.example.demo.consumer;

import org.springframework.core.annotation.Order;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    @KafkaListener(topics = "orders",groupId = "group1")
    public void listen(Order order) {
        System.out.println("Received: "+order);
    }
}

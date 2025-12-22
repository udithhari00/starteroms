package com.example.demo.producer;

import com.example.demo.avro.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/produce")
public class ProducerController {

    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;

    @PostMapping
    public String sendOrder(@RequestBody Order order) {
        kafkaTemplate.send("orders", order.getOrderId().toString(), order);
        return "Sent";
    }
}

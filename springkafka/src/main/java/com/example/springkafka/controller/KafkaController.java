package com.example.springkafka.controller;


import com.example.springkafka.service.KafkaProducerService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaController {

    private final KafkaProducerService kafkaProducerService;
    public KafkaController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    @GetMapping("/send")
    public String sendMessage(@RequestParam String message){
        kafkaProducerService.sendMessage(message);
        return "Message sent to topic: " + message;
    }
}

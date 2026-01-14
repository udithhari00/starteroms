package org.example.mongodb_spring.controller;

import org.example.mongodb_spring.document.OrderLifeCycleDocument;
import org.example.mongodb_spring.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderLifeCycleController {

    private final OrderService service;
    public OrderLifeCycleController(OrderService orderservice) {
        service = orderservice;
    }

    @PostMapping("/{orderId}/events/{eventType}")
    public void recordEvent(
            @PathVariable String orderId,
            @PathVariable String eventType,
            @RequestBody Map<String, Object> payload) {

        service.recordEvent(orderId, eventType, payload);
    }

    @GetMapping("/{orderId}")
    public OrderLifeCycleDocument getLifecycle(@PathVariable String orderId) {
        return service.getLifeCycle(orderId);
    }
}
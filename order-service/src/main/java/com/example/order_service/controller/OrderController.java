package com.example.order_service.controller;

import com.example.order_service.document.OrderEventLog;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.service.OrderService;
import com.example.orderservice.avro.OrderCreatedEvent;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request){
        log.info("Received create order request for customer: {}",request.getCustomerId());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId){
        log.info("Fetching order with id {}",orderId);
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable String customerId){

        log.info("Fetching orders for customer {}",customerId);
        List<OrderResponse> responses = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok().body(responses);
    }

    @GetMapping("/{orderId}/events")
    public ResponseEntity<List<OrderEventLog>> getOrderEvents(@PathVariable String orderId){

        log.info("Fetching events for order with id {}",orderId);
        List<OrderEventLog> events = orderService.getOrderEvents(orderId);
        return ResponseEntity.ok().body(events);
    }
}

package com.example.order_service.service;

import com.example.order_service.document.OrderEventLog;
import com.example.order_service.dto.CreateOrderRequest;
import com.example.order_service.dto.OrderItemRequest;
import com.example.order_service.dto.OrderItemResponse;
import com.example.order_service.dto.OrderResponse;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.OrderItem;
import com.example.order_service.entity.OrderStatus;
import com.example.order_service.repository.OrderEventLogRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.orderservice.avro.OrderCreatedEvent;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderEventLogRepository eventLogRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    @Value("${app.kafka.topic.order-created}")
    private String orderCreatedTopic;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request){
        log.info("Creating Order for customer: {}",request.getCustomerId());

        BigDecimal totalAmount = request.getItems().stream()
                .map(item ->item.getUnitPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

//      For creating and saving of OrderEntity
        Order order= Order.builder()
                .customerId(request.getCustomerId())
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .build();

//        Adding items to the order
        for(OrderItemRequest orderItemRequest : request.getItems()){
            OrderItem item= OrderItem.builder()
                    .productId(orderItemRequest.getProductId())
                    .productName(orderItemRequest.getProductName())
                    .quantity(orderItemRequest.getQuantity())
                    .unitPrice(orderItemRequest.getUnitPrice())
                    .build();
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved to PostgreSQL with ID: {}",savedOrder.getId());

//        Create and publish Avro event to Kafka
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = buildAvroEvent(savedOrder, eventId, request.getCorrelationId());

        kafkaTemplate.send(orderCreatedTopic, savedOrder.getId(),event);
        log.info("OrderCreatedEvent published to Kafka topic: {}",orderCreatedTopic);

//        Storing event logs in MongoDB
        saveEventLog(savedOrder,eventId,request.getCorrelationId());

        return mapToResponse(savedOrder);
    }

    private OrderCreatedEvent buildAvroEvent(Order order, String eventId, String correlationId){

        List<com.example.orderservice.avro.OrderItem
                > avroItems = order.getItems().stream()
                .map(item -> com.example.orderservice.avro.OrderItem.newBuilder()
                        .setProductId(item.getProductId())
                        .setProductName(item.getProductName())
                        .setQuantity(item.getQuantity())
                        .setUnitPrice(item.getUnitPrice().doubleValue())
                        .build())
                .collect(Collectors.toList());

        return OrderCreatedEvent.newBuilder()
                .setEventId(eventId)
                .setOrderId(order.getId())
                .setCustomerId(order.getCustomerId())
                .setTotalAmount(order.getTotalAmount().doubleValue())
                .setItems(avroItems)
                .setTimestamp(Instant.ofEpochMilli(Instant.now().toEpochMilli()))
                .setCorrelationId(correlationId)
                .build();

    }

    private void saveEventLog(Order order, String eventId, String correlationId){
        Map<String,Object> payload = new HashMap<>();
        payload.put("orderId",order.getId());
        payload.put("customerId",order.getCustomerId());
        payload.put("totalAmount",order.getTotalAmount().doubleValue());
        payload.put("status",order.getStatus().name());
        payload.put("itemCount",order.getItems().size());

        OrderEventLog eventLog = OrderEventLog.builder()
                .eventId(eventId)
                .eventType("OrderCreated")
                .orderId(order.getId())
                .aggregateType("Order")
                .payload(payload)
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();

        eventLogRepository.save(eventLog);
        log.info("Event log saved to MongoDB for order: {}",order.getId());
    }
    public OrderResponse getOrderById(String orderId){
        return orderRepository.findById(orderId)
                .map(this::mapToResponse)
                .orElseThrow(()-> new RuntimeException("Order not found : "+orderId));
    }

    public List<OrderResponse> getOrdersByCustomerId(String customerId){
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<OrderEventLog> getOrderEvents(String orderId){
        return eventLogRepository.findByOrderIdOrderByTimestampDesc(orderId);
    }

    private OrderResponse mapToResponse(Order order){
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item->OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

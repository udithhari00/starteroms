package org.example.mongodb_spring.service;

import org.example.mongodb_spring.document.OrderEvent;
import org.example.mongodb_spring.document.OrderLifeCycleDocument;
import org.example.mongodb_spring.repository.OrderLifecycleRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderService {

    private final OrderLifecycleRepository orderLifecycleRepository;

    public OrderService(OrderLifecycleRepository orderLifecycleRepository) {
        this.orderLifecycleRepository = orderLifecycleRepository;
    }

    public void recordEvent(String orderId, String eventType, Map<String, Object> payload) {
        OrderLifeCycleDocument orderLifeCycleDocument = orderLifecycleRepository.findById(orderId)
                .orElseGet(()->{
                    OrderLifeCycleDocument d=new OrderLifeCycleDocument();
                    d.setOrderId(orderId);
                    return d;
                });
        orderLifeCycleDocument.addEvent(new OrderEvent(eventType,payload));
        orderLifecycleRepository.save(orderLifeCycleDocument);
    }

    public OrderLifeCycleDocument getLifeCycle(String orderId) {
        return orderLifecycleRepository.findById(orderId).orElseThrow(()->new RuntimeException("Order Not Found"));
    }
}

package com.example.order_service.repository;

import com.example.order_service.document.OrderEventLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventLogRepository extends MongoRepository<OrderEventLog,String> {

    List<OrderEventLog> findByOrderIdOrderByTimestampDesc(String orderId);
    List<OrderEventLog> findByEventType(String eventType);
    List<OrderEventLog> findByCorrelationId(String correlationId);
}
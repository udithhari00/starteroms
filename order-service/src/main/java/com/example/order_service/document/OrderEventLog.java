package com.example.order_service.document;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "order_events")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEventLog {

    @Id
    private String id;

    private String orderId;
    private String eventId;
    private String eventType;
    private String aggregateType;
    private Map<String,Object> payload;
    private Instant timestamp;
    private String correlationId;

}

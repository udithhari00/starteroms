package org.example.mongodb_spring.document;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "order_lifecycle")
public class OrderLifeCycleDocument {

    @Id
    private String orderId;

    private List<OrderEvent> events = new ArrayList<>();

    private Instant createdAt = Instant.now();

    public void addEvent(OrderEvent event){
        this.events.add(event);
    }
}

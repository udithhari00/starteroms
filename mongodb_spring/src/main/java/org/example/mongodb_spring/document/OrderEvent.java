package org.example.mongodb_spring.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    private String type;
    private Instant occuredAt;
    private Map<String, Object> payload;

    public OrderEvent(String type, Map<String, Object> payload) {
        this.type = type;
        this.payload = payload;
        this.occuredAt = Instant.now();
    }

}

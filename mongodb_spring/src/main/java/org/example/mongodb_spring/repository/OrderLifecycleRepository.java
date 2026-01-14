package org.example.mongodb_spring.repository;

import org.example.mongodb_spring.document.OrderLifeCycleDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderLifecycleRepository extends MongoRepository<OrderLifeCycleDocument, String> {
}

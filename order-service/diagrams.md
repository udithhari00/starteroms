# Order Service - ER Diagram and Flowchart

## 1. Entity-Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ENTITY RELATIONSHIP DIAGRAM                        │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────┐         ┌─────────────────────────────┐
│          ORDER              │         │        ORDER_ITEM           │
├─────────────────────────────┤         ├─────────────────────────────┤
│ PK  id          VARCHAR(36) │───┐     │ PK  id          VARCHAR(36) │
│     customer_id VARCHAR(255)│   │     │ FK  order_id    VARCHAR(36) │──┐
│     total_amount DECIMAL    │   │     │     product_id  VARCHAR(255)│  │
│     status      VARCHAR(50) │   │     │     product_name VARCHAR    │  │
│     created_at  TIMESTAMP   │   │     │     quantity    INTEGER     │  │
│     updated_at  TIMESTAMP   │   │     │     unit_price  DECIMAL     │  │
└─────────────────────────────┘   │     └─────────────────────────────┘  │
              │                   │                                      │
              │ 1                 └──────────────── * ────────────────────┘
              │                        (One-to-Many)
              │
              │ 1
              │
              ▼ *
┌─────────────────────────────┐
│   ORDER_EVENT_LOG (MongoDB) │
├─────────────────────────────┤
│ PK  _id         ObjectId    │
│     orderId     String      │◄─── Logical reference (not FK)
│     eventId     String      │
│     eventType   String      │
│     aggregateType String    │
│     payload     Object      │
│     timestamp   Instant     │
│     correlationId String    │
└─────────────────────────────┘


┌─────────────────────────────┐
│       ORDER_STATUS          │
├─────────────────────────────┤
│  CREATED                    │
│  PAYMENT_PENDING            │
│  PAYMENT_COMPLETED          │
│  COMPLETED                  │
│  CANCELED                   │
└─────────────────────────────┘
```

### Relationships Summary

| Relationship | Type | Description |
|--------------|------|-------------|
| Order → OrderItem | One-to-Many | An order contains multiple items (CascadeType.ALL, orphanRemoval=true) |
| Order → OrderEventLog | One-to-Many | Each order can have multiple event logs (logical reference via orderId) |

---

## 2. Application Flowchart

### 2.1 Order Creation Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ORDER CREATION FLOWCHART                             │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────┐
                              │   CLIENT    │
                              └──────┬──────┘
                                     │
                                     ▼
                    ┌────────────────────────────────┐
                    │  POST /api/orders              │
                    │  Body: CreateOrderRequest      │
                    │  - customerId                  │
                    │  - items[]                     │
                    │  - correlationId (optional)   │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │      OrderController           │
                    │      createOrder()             │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │       OrderService             │
                    │       createOrder()            │
                    │       @Transactional           │
                    └───────────────┬────────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
    ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
    │ Calculate Total │   │  Build Order    │   │ Build OrderItems│
    │ Amount          │   │  Entity         │   │                 │
    │ Σ(qty * price)  │   │  - Generate ID  │   │ For each item:  │
    └────────┬────────┘   │  - Set customer │   │ - Generate ID   │
             │            │  - Set status   │   │ - Set product   │
             │            │    = CREATED    │   │ - Set quantity  │
             │            └────────┬────────┘   │ - Set price     │
             │                     │            └────────┬────────┘
             └─────────────────────┼─────────────────────┘
                                   │
                                   ▼
                    ┌────────────────────────────────┐
                    │    Save Order to PostgreSQL    │
                    │    orderRepository.save()      │
                    │    (Cascade saves OrderItems)  │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │   Build Avro Event             │
                    │   OrderCreatedEvent            │
                    │   - eventId (UUID)             │
                    │   - orderId                    │
                    │   - customerId                 │
                    │   - totalAmount                │
                    │   - items[]                    │
                    │   - timestamp                  │
                    │   - correlationId              │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │   Publish to Kafka             │
                    │   Topic: "order.created"       │
                    │   Key: orderId                 │
                    │   Value: Avro serialized event │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │   Save Event Log to MongoDB    │
                    │   Collection: order_events     │
                    │   - orderId                    │
                    │   - eventType: "OrderCreated"  │
                    │   - payload: order data        │
                    │   - timestamp                  │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                    ┌────────────────────────────────┐
                    │   Map to OrderResponse DTO     │
                    │   Return HTTP 201 (CREATED)    │
                    └───────────────┬────────────────┘
                                    │
                                    ▼
                              ┌─────────────┐
                              │   CLIENT    │
                              │  (Response) │
                              └─────────────┘
```

### 2.2 Order Retrieval Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ORDER RETRIEVAL FLOWCHART                            │
└─────────────────────────────────────────────────────────────────────────────┘

    GET /api/orders/{orderId}          GET /api/orders/customer/{customerId}
              │                                       │
              ▼                                       ▼
    ┌─────────────────┐                    ┌─────────────────┐
    │ OrderController │                    │ OrderController │
    │   getOrder()    │                    │getOrdersByCustomer│
    └────────┬────────┘                    └────────┬────────┘
             │                                      │
             ▼                                      ▼
    ┌─────────────────┐                    ┌─────────────────┐
    │  OrderService   │                    │  OrderService   │
    │ getOrderById()  │                    │getOrdersByCustomerId│
    └────────┬────────┘                    └────────┬────────┘
             │                                      │
             ▼                                      ▼
    ┌─────────────────┐                    ┌─────────────────┐
    │ OrderRepository │                    │ OrderRepository │
    │   findById()    │                    │findByCustomerId()│
    └────────┬────────┘                    └────────┬────────┘
             │                                      │
             ▼                                      ▼
    ┌─────────────────┐                    ┌─────────────────┐
    │   PostgreSQL    │                    │   PostgreSQL    │
    │   orders table  │                    │   orders table  │
    └────────┬────────┘                    └────────┬────────┘
             │                                      │
             ▼                                      ▼
    ┌─────────────────┐                    ┌─────────────────┐
    │ Map to Response │                    │Map to Response[]│
    └────────┬────────┘                    └────────┬────────┘
             │                                      │
             ▼                                      ▼
         Response                              Response
```

### 2.3 Get Order Events Flow

```
    GET /api/orders/{orderId}/events
              │
              ▼
    ┌─────────────────┐
    │ OrderController │
    │getOrderEvents() │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │  OrderService   │
    │getOrderEvents() │
    └────────┬────────┘
             │
             ▼
    ┌───────────────────────┐
    │OrderEventLogRepository│
    │findByOrderIdOrdered() │
    └────────┬──────────────┘
             │
             ▼
    ┌─────────────────┐
    │    MongoDB      │
    │ order_events    │
    └────────┬────────┘
             │
             ▼
    ┌─────────────────┐
    │List<OrderEventLog>│
    └────────┬────────┘
             │
             ▼
         Response
```

---

## 3. System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         SYSTEM ARCHITECTURE                                  │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────┐
                              │   Client    │
                              └──────┬──────┘
                                     │ REST API
                                     ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                         ORDER SERVICE (Port 8083)                           │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         Controller Layer                             │   │
│  │                        OrderController                               │   │
│  └───────────────────────────────┬─────────────────────────────────────┘   │
│                                  │                                          │
│  ┌───────────────────────────────┴─────────────────────────────────────┐   │
│  │                         Service Layer                                │   │
│  │                         OrderService                                 │   │
│  └───────────────────────────────┬─────────────────────────────────────┘   │
│                                  │                                          │
│  ┌───────────────────────────────┴─────────────────────────────────────┐   │
│  │                       Repository Layer                               │   │
│  │         OrderRepository              OrderEventLogRepository         │   │
│  └───────────────────────────────┬─────────────────────────────────────┘   │
│                                  │                                          │
│  ┌───────────────────────────────┴─────────────────────────────────────┐   │
│  │                         Config Layer                                 │   │
│  │                         KafkaConfig                                  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────┬───────────────────────────────────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
         ▼                           ▼                           ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│   PostgreSQL    │       │     MongoDB     │       │      Kafka      │
│    (Port 5432)  │       │   (Port 27017)  │       │   (Port 9092)   │
├─────────────────┤       ├─────────────────┤       ├─────────────────┤
│  Database:      │       │  Database:      │       │  Topic:         │
│  orderdb        │       │  orderdb        │       │  order.created  │
├─────────────────┤       ├─────────────────┤       └────────┬────────┘
│  Tables:        │       │  Collection:    │                │
│  - orders       │       │  - order_events │                │
│  - order_items  │       └─────────────────┘                │
└─────────────────┘                                          │
                                                             ▼
                                                   ┌─────────────────┐
                                                   │ Schema Registry │
                                                   │   (Port 8081)   │
                                                   ├─────────────────┤
                                                   │ Avro Schemas:   │
                                                   │ OrderCreatedEvent│
                                                   └─────────────────┘
```

---

## 4. Data Flow Summary

| Step | Action | Source | Destination | Data |
|------|--------|--------|-------------|------|
| 1 | Create Order | Client | OrderController | CreateOrderRequest |
| 2 | Process Order | OrderController | OrderService | CreateOrderRequest |
| 3 | Save Order | OrderService | PostgreSQL | Order + OrderItems |
| 4 | Publish Event | OrderService | Kafka | OrderCreatedEvent (Avro) |
| 5 | Log Event | OrderService | MongoDB | OrderEventLog |
| 6 | Return Response | OrderService | Client | OrderResponse |

---

## 5. Technology Stack

| Component | Technology | Purpose |
|-----------|------------|---------|
| Application | Spring Boot 4.0.1 | Main framework |
| Language | Java 21 | Programming language |
| Relational DB | PostgreSQL 15 | Order persistence |
| Document DB | MongoDB 7 | Event logging |
| Message Broker | Apache Kafka | Event streaming |
| Serialization | Apache Avro | Message format |
| Schema Management | Confluent Schema Registry | Schema validation |
| ORM | Spring Data JPA | Database access |
| Container | Docker Compose | Infrastructure |

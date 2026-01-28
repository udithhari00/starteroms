package org.example.payment_service.consumer;


import com.example.orderservice.avro.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.entity.PaymentStatus;
import org.example.payment_service.service.PaymentService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${kafka.topic.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumerOrderCreatedEvent(OrderCreatedEvent event){
        log.info("Received OrderCreatedEvent: orderId={}, customerId={}, amount={}",
                event.getOrderId(),
                event.getCustomerId(),
                event.getTotalAmount());

        try{
            paymentService.processOrderEvent(event);
        }catch(Exception e){
            log.error("Error processing order event: {}",event.getOrderId(), e);
            throw e;
        }
    }


}

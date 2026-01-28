package org.example.payment_service.service;

import com.example.orderservice.avro.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.payment_service.entity.Payment;
import org.example.payment_service.entity.PaymentStatus;
import org.example.payment_service.repository.PaymentRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepo paymentRepo;

    @Transactional
    public Payment processOrderEvent(OrderCreatedEvent event){
        log.info("Processing payment for order: {}", event.getOrderId());

        Optional<Payment> existingPayment =  paymentRepo.findByOrderId(event.getOrderId());
        if(existingPayment.isPresent()){
            log.warn("Payment already exists for order: {}", event.getOrderId());
            return existingPayment.get();
        }
        Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .customerId(event.getCustomerId())
                .amount(BigDecimal.valueOf(event.getTotalAmount()))
                .status(PaymentStatus.PENDING)
                .correlationId(event.getCorrelationId())
                .createdAt(LocalDateTime.now())
                .build();
        Payment savedPayment = paymentRepo.save(payment);
        log.info("Payment created with ID: {} for order: {}", savedPayment.getId(),savedPayment.getOrderId());
        processPayment(savedPayment);
        return savedPayment;
    }

    @Transactional
    public void processPayment(Payment payment){
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepo.save(payment);

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepo.save(payment);

        log.info("Payment completed with ID: {} for order: {}", payment.getId(),payment.getOrderId());
    }

    public Optional<Payment> getPaymentById(UUID id){
        return paymentRepo.findById(id);
    }

    public Optional<Payment> getPaymentByOrderId(String orderId){
        return paymentRepo.findByOrderId(orderId);
    }

    public List<Payment> getPaymentByCustomerId(String customerId){
        return paymentRepo.findByCustomerId(customerId);
    }
}

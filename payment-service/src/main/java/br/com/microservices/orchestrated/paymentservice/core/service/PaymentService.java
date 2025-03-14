package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class PaymentService {

    private final static Double REDUCE_SUM_VALUE = 0.0;

    private final static String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private KafkaProducer kafkaProducer;

    private PaymentRepository paymentRepository;

    private final JsonUtil jsonUtil;


    public void realizePayment(Event event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);
        } catch (Exception e) {
            log.error("Error trying to make payment", e);
            kafkaProducer.sendEvent(jsonUtil.toJson(event));
        }
    }

    private void createPendingPayment(Event event) {
        var totalAmount = calculateTotalAmount(event);
        var totalItems = calculateTotalItems(event);

        var payment = Payment.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    private int calculateTotalItems(Event event) {
        return event
                .getPayload()
                .getOrderProducts()
                .stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    private double calculateTotalAmount(Event event) {
        return event
                .getPayload()
                .getOrderProducts()
                .stream()
                .map(products ->
                         products.getQuantity() * products.getProduct().getUnitValue() * 1.0)
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private void checkCurrentValidation(Event event) {
        if (paymentRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("There's another transcationId for this validation");
        }
    }

    private void setEventAmountItems(Event event, Payment payment){
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItens(payment.getTotalItems());
    }

    private void save(Payment payment) {
        paymentRepository.save(payment);
    }
}

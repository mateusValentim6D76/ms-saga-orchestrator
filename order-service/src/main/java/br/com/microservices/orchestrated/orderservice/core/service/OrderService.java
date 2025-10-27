package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.document.Order;
import br.com.microservices.orchestrated.orderservice.core.dto.OrderRequestDTO;
import br.com.microservices.orchestrated.orderservice.core.producer.SagaProducer;
import br.com.microservices.orchestrated.orderservice.core.repository.OrderRepository;
import br.com.microservices.orchestrated.orderservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private static final String TRANSACTION_ID_PATTERN = "%s_%s";

    private final EventService eventService;
    private final SagaProducer producer;
    private final JsonUtil jsonUtil;
    private final OrderRepository repository;

    public OrderService(EventService eventService, SagaProducer producer, JsonUtil jsonUtil, OrderRepository repository) {
        this.eventService = eventService;
        this.producer = producer;
        this.jsonUtil = jsonUtil;
        this.repository = repository;
    }

    public Order createOrder(OrderRequestDTO orderRequest) {
        var order = new Order();
        order.setOrderProducts(orderRequest.getOrderProducts());
        order.setCreatedAt(LocalDateTime.now());
        order.setTransactionId(String.format(TRANSACTION_ID_PATTERN, Instant.now().toEpochMilli(), UUID.randomUUID()));

        repository.save(order);
        producer.sendEvent(jsonUtil.toJson(createPayload(order)));
        return order;
    }

    private Event createPayload(Order order) {
        var event = new Event();
        event.setOrderId(order.getId());
        event.setTransactionId(order.getTransactionId());
        event.setPayload(order);
        event.setCreatedAt(LocalDateTime.now());

        eventService.save(event);
        return event;
    }
}

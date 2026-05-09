package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFiltersDTO;
import br.com.microservices.orchestrated.orderservice.core.repository.EventRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import static org.springframework.util.ObjectUtils.isEmpty;


@Service
@AllArgsConstructor
@Slf4j
public class EventService  {

    private EventRepository eventRepository;

    public void notifyEnding(Event event){
        event.setOrderId(event.getOrderId());
        event.setCreatedAt(LocalDateTime.now());
        save(event);
        log.info("Order {} with saga notified@ transactionId: {}", event.getOrderId(), event.getTransactionId());
    }

    public List<Event> findAll(){
        return eventRepository.findAllByOrderByCreatedAtDesc();
    }

    private Event save(Event event) {
        return eventRepository.save(event);
    }

    public Event findByFilters(EventFiltersDTO filters) {
        validateEmptyFilters(filters);
        if (!isEmpty(filters.orderId())) {
            return findByOrderId(filters.orderId());
        } else {
            return findByTransactionId(filters.transactionId());
        }
    }

    private void validateEmptyFilters(EventFiltersDTO filters) {
        if (isEmpty(filters.orderId()) && isEmpty(filters.transactionId())) {
            throw new ValidationException("OrderID or TransactionID must be informed.");
        }
    }

    private Event findByTransactionId(String transactionId) {
        return eventRepository
                .findTop1ByTransactionIdOrderByCreatedAtDesc(transactionId)
                .orElseThrow(() -> new ValidationException("Event not found by transactionId."));
    }

    private Event findByOrderId(String orderId) {
        return eventRepository
                .findTop1ByOrderIdOrderByCreatedAtDesc(orderId)
                .orElseThrow(() -> new ValidationException("Event not found by orderID."));
    }
}

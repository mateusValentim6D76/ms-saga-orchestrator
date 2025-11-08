package br.com.microservices.orchestrated.orderservice.core.service.impl;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import org.springframework.data.mongodb.repository.MongoRepository;

public class EventServiceImpl implements MongoRepository<Event, String> {
}

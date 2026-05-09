package br.com.microservices.orchestrated.orderservice.core.controller;

import br.com.microservices.orchestrated.orderservice.core.document.Event;
import br.com.microservices.orchestrated.orderservice.core.dto.EventFiltersDTO;
import br.com.microservices.orchestrated.orderservice.core.service.EventService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/event")
@AllArgsConstructor
public class EventController {

    private EventService eventService;

    @GetMapping
    public Event findByFilters(EventFiltersDTO filters) {
        return eventService.findByFilters(filters);
    }

    @GetMapping("/all")
    public List<Event> findAll() {
        return eventService.findAll();
    }

}

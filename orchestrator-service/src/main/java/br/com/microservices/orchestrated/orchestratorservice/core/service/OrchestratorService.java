package br.com.microservices.orchestrated.orchestratorservice.core.service;

import br.com.microservices.orchestrated.orchestratorservice.core.dto.Event;
import br.com.microservices.orchestrated.orchestratorservice.core.dto.History;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.EEventSource;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics;
import br.com.microservices.orchestrated.orchestratorservice.core.producer.SagaOrchestratorProducer;
import br.com.microservices.orchestrated.orchestratorservice.core.utils.JsonUtil;
import br.com.microservices.orchestrated.orchestratorservice.saga.SagaExecutionController;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.orchestratorservice.core.enums.ETopics.NOTIFY_ENDINDG;

@Service
@Slf4j
@AllArgsConstructor
public class OrchestratorService {

    private final SagaOrchestratorProducer sagaOrchestratorProducer;

    private final JsonUtil jsonUtil;

    private final SagaExecutionController sagaExecutionController;

    public void startSaga(Event event) {
        event.setSource(EEventSource.ORCHESTRATOR);
        event.setStatus(ESagaStatus.SUCCESS);
        var topic = getTopics(event);
        log.info("### SAGA STARTED ###");
        addHistory(event, "SAGA_STARTD");
        sendToProducerWithTopic(event, topic);
    }

    public void finishSagaSuccess(Event event) {
        event.setSource(EEventSource.ORCHESTRATOR);
        event.setStatus(ESagaStatus.SUCCESS);
        log.info("### SAGA FINISHED SUCCESSFULLY FOR EVENT {} ###", event.getId());
        addHistory(event, "Saga finished successfully");
        notifyFinishedSaga(event);
    }

    public void finishSagaFail(Event event) {
        event.setSource(EEventSource.ORCHESTRATOR);
        event.setStatus(ESagaStatus.FAIL);
        log.info("### SAGA FINISHED WITH ERRORS FOR EVENT {} ###", event.getId());
        addHistory(event, "Saga finished with errors");
        notifyFinishedSaga(event);
    }

    public void continueSaga(Event event) {
        var topic = getTopics(event);
        log.info("### SAGA CONTINUING FOR EVENT {} ###", event.getId());
        sendToProducerWithTopic(event, topic);
    }

    private void sendToProducerWithTopic(Event event, ETopics topic) {
        sagaOrchestratorProducer.sendEvent(jsonUtil.toJson(event), topic.getTopic());
    }

    private ETopics getTopics(Event event){
        return sagaExecutionController.getNextTopic(event);
    }

    private void notifyFinishedSaga(Event event) {
        sendToProducerWithTopic(event, NOTIFY_ENDINDG);
    }

    private void addHistory(Event event, String message) {
        var history = History
                .builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createAt(LocalDateTime.now())
                .build();
        event.addToHistoryList(history);
    }
}

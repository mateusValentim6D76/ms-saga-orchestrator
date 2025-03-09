package br.com.microservices.orchestrated.productvalidationservice.core.service;

import br.com.microservices.orchestrated.productvalidationservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.EventDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.HistoryDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.dto.OrderProductsDTO;
import br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus;
import br.com.microservices.orchestrated.productvalidationservice.core.models.Validation;
import br.com.microservices.orchestrated.productvalidationservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ProductRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.repository.ValidationRepository;
import br.com.microservices.orchestrated.productvalidationservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.productvalidationservice.core.enums.ESagaStatus.*;
import static org.apache.logging.log4j.ThreadContext.isEmpty;

@Service
@Slf4j
@AllArgsConstructor
public class ProductValidationService {

    private final static String CURRENT_SOURCE = "PRODUCT_VALIDATION_SERVICE";

    private KafkaProducer kafkaProducer;

    private ProductRepository productRepository;

    private ValidationRepository validationRepository;

    private final JsonUtil jsonUtil;

    public void validateExistingProduct(EventDTO event) {
        try {
            checkCurrentValidation(event);
            createValidation(event, true);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error to trying to validate products.", e);
            handleFailNotExecuted(event, e.getMessage());
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    public void rollbackEvent(EventDTO event){
        changeValidationToFail(event);
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Rollback executed on product validation!");
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void changeValidationToFail(EventDTO event) {
        validationRepository.findByOrderIdAndTransactionId(event.getPayload().getId(),
                event.getTransactionId())
                .ifPresentOrElse(validation -> {
                    validation.setSuccess(false);
                    validationRepository.save(validation);
                },
                        () -> createValidation(event, false));
    }

    private void handleFailNotExecuted(EventDTO event, String message) {
        event.setStatus(ROLLBACK_PEDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to validade products. ".concat(message));
    }

    private void checkCurrentValidation(EventDTO event) {
        validateProductsInformed(event);
        if (validationRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())){
            throw new ValidationException("There's another transcationId for this validation");
        }
        event.getPayload().getOrderProducts().forEach(product -> {
            validateProductInformed(product);
            validateExistingProduct(product.getProduct().getCode());

        });
    }

    private void validateProductInformed(OrderProductsDTO product) {
        if (ObjectUtils.isEmpty(product.getProduct()) || ObjectUtils.isEmpty(product.getProduct().getCode())){
            throw new ValidationException("Product must be informed!");
        }
    }

    private void validateProductsInformed(EventDTO event) {
        if (ObjectUtils.isEmpty(event.getPayload()) || ObjectUtils.isEmpty(event.getPayload().getOrderProducts())) {
            throw new ValidationException("Product list is empty.");
        }
        if (ObjectUtils.isEmpty(event.getPayload().getTransactionId()) || ObjectUtils.isEmpty(event.getId())) {
            throw new ValidationException("Order Id and Transaction Id must be informed.");
        }
    }

    private void validateExistingProduct(String code) {
        if (!productRepository.existsByCode(code)) {
            throw new ValidationException("Product does not exists in database");
        }
    }

    private void createValidation(EventDTO eventDTO, boolean success) {
        var validation = Validation.builder()
                .orderId(eventDTO.getOrderId())
                .transactionId(eventDTO.getTransactionId())
                .success(success)
                .build();

        validationRepository.save(validation);
    }

    private void handleSuccess(EventDTO event){
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Products are validates successfully.");
    }

    private void addHistory(EventDTO event, String message){
        var history = HistoryDTO.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createAt(LocalDateTime.now())
                .build();

        event.addHistory(history);
    }
}
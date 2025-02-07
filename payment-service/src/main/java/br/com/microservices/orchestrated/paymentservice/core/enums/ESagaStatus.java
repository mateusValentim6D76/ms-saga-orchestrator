package br.com.microservices.orchestrated.paymentservice.core.enums;

import lombok.Getter;

@Getter
public enum ESagaStatus {

    SUCCESS,
    ROLLBACK_PEDING,
    FAIL;
}

package br.com.microservices.orchestrated.productvalidationservice.core.enums;

import lombok.Getter;

@Getter
public enum ESagaStatus {

    SUCCESS,
    ROLLBACK_PEDING,
    FAIL;
}

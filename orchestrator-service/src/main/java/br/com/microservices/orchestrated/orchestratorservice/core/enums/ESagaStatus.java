package br.com.microservices.orchestrated.orchestratorservice.core.enums;

import lombok.Getter;

@Getter
public enum ESagaStatus {

    SUCCESS,
    ROLLBACK_PENDING,
    FAIL;

    ESagaStatus() {
    }
}

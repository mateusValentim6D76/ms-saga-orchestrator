package br.com.microservices.orchestrated.inventoryservice.core.enums;

import lombok.Getter;

@Getter
public enum ESagaStatus {

    SUCCESS,
    ROLLBACK_PEDING,
    FAIL;
}

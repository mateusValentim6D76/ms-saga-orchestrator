package br.com.microservices.orchestrated.orchestratorservice.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public enum ESagaStatus {

    SUCCESS,
    ROLLBACK_PEDING,
    FAIL;
}

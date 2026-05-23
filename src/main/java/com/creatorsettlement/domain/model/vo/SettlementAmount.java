package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

import java.math.BigDecimal;

public record SettlementAmount(BigDecimal value) {

    public SettlementAmount {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_AMOUNT_NULL.message());
        }
    }

    public static SettlementAmount of(BigDecimal value) {
        return new SettlementAmount(value);
    }
}

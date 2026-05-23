package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

import java.math.BigDecimal;

public record FeeRate(BigDecimal value) {

    public FeeRate {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.FEE_RATE_NULL.message());
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(DomainErrorMessage.FEE_RATE_NEGATIVE.message());
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(DomainErrorMessage.FEE_RATE_GREATER_THAN_ONE.message());
        }
    }

    public static FeeRate of(BigDecimal value) {
        return new FeeRate(value);
    }

    public static FeeRate defaultRate() {
        return new FeeRate(new BigDecimal("0.2"));
    }
}

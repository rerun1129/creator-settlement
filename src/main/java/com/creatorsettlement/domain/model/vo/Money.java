package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

import java.math.BigDecimal;

public record Money(BigDecimal value) {

    public Money {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.MONEY_NULL.message());
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(DomainErrorMessage.MONEY_NOT_POSITIVE.message());
        }
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }
}

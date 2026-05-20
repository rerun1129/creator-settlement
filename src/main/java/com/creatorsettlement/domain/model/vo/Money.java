package com.creatorsettlement.domain.model.vo;

import java.math.BigDecimal;

public record Money(BigDecimal value) {

    public Money {
        if (value == null) {
            throw new IllegalArgumentException("금액은 null일 수 없습니다");
        }
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다");
        }
    }
}

package com.creatorsettlement.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SettlementAmount 값 객체 단위 테스트")
class SettlementAmountTest {

    @Test
    @DisplayName("양수 값을 허용한다")
    void accepts_positive_value() {
        // when
        SettlementAmount amount = SettlementAmount.of(new BigDecimal("100000"));

        // then
        assertThat(amount.value()).isEqualByComparingTo(new BigDecimal("100000"));
    }

    @Test
    @DisplayName("0을 허용한다")
    void accepts_zero() {
        // when
        SettlementAmount amount = SettlementAmount.of(BigDecimal.ZERO);

        // then
        assertThat(amount.value()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("음수 값을 허용한다")
    void accepts_negative_value() {
        // when
        SettlementAmount amount = SettlementAmount.of(new BigDecimal("-40000"));

        // then
        assertThat(amount.value()).isEqualByComparingTo(new BigDecimal("-40000"));
    }

    @Test
    @DisplayName("null 값은 거부된다")
    void rejects_null_value() {
        // when
        // then
        assertThatThrownBy(() -> SettlementAmount.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

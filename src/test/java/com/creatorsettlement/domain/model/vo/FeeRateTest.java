package com.creatorsettlement.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeeRate 값 객체 단위 테스트")
class FeeRateTest {

    @Test
    @DisplayName("null 값은 거부된다")
    void rejects_null_value() {
        // when
        // then
        assertThatThrownBy(() -> FeeRate.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("음수 값은 거부된다")
    void rejects_negative_value() {
        // given
        BigDecimal negative = new BigDecimal("-0.01");

        // when
        // then
        assertThatThrownBy(() -> FeeRate.of(negative))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("1을 초과하면 거부된다")
    void rejects_value_greater_than_one() {
        // given
        BigDecimal overOne = new BigDecimal("1.0001");

        // when
        // then
        assertThatThrownBy(() -> FeeRate.of(overOne))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("0(0%)은 허용된다")
    void accepts_zero() {
        // when
        FeeRate feeRate = FeeRate.of(BigDecimal.ZERO);

        // then
        assertThat(feeRate.value()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("1(100%)은 허용된다")
    void accepts_one() {
        // when
        FeeRate feeRate = FeeRate.of(BigDecimal.ONE);

        // then
        assertThat(feeRate.value()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("표준 20% 수수료율을 허용한다")
    void accepts_typical_twenty_percent() {
        // given
        BigDecimal twentyPercent = new BigDecimal("0.2000");

        // when
        FeeRate feeRate = FeeRate.of(twentyPercent);

        // then
        assertThat(feeRate.value()).isEqualByComparingTo(twentyPercent);
    }
}

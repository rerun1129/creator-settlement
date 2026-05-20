package com.creatorsettlement.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money 값 객체 단위 테스트")
class MoneyTest {

    @Test
    @DisplayName("정상 양수 값이면 Money가 생성된다")
    void should_create_money_when_value_is_positive() {
        // given
        BigDecimal value = new BigDecimal("10000");

        // when
        Money money = new Money(value);

        // then
        assertThat(money.value()).isEqualByComparingTo(value);
    }

    @Test
    @DisplayName("값이 0이면 예외가 발생한다")
    void should_throw_when_value_is_zero() {
        // given
        BigDecimal zeroValue = BigDecimal.ZERO;

        // when & then
        assertThatThrownBy(() -> new Money(zeroValue)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("값이 음수이면 예외가 발생한다")
    void should_throw_when_value_is_negative() {
        // given
        BigDecimal negativeValue = new BigDecimal("-1");

        // when & then
        assertThatThrownBy(() -> new Money(negativeValue)).isInstanceOf(IllegalArgumentException.class);
    }
}

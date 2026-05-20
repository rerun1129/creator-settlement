package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OccurredAt 값 객체 단위 테스트")
class OccurredAtTest {

    @Test
    @DisplayName("과거 시점이면 OccurredAt이 생성된다")
    void should_create_occurred_at_when_value_is_past() {
        // given
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);

        // when
        OccurredAt occurredAt = OccurredAt.of(past);

        // then
        assertThat(occurredAt.value()).isEqualTo(past);
    }

    @Test
    @DisplayName("값이 null이면 예외가 발생한다")
    void should_throw_when_value_is_null() {
        // given (별도 변수 불필요)

        // when & then
        assertThatThrownBy(() -> OccurredAt.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(DomainErrorMessage.OCCURRED_AT_NULL.message());
    }

    @Test
    @DisplayName("미래 시점이면 예외가 발생한다")
    void should_throw_when_value_is_in_future() {
        // given
        LocalDateTime future = LocalDateTime.now().plusMinutes(1);

        // when & then
        assertThatThrownBy(() -> OccurredAt.of(future))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(DomainErrorMessage.OCCURRED_AT_FUTURE.message());
    }

    @Test
    @DisplayName("같은 값이면 두 OccurredAt 인스턴스는 동등하다")
    void should_be_equal_when_values_are_same() {
        // given
        LocalDateTime value = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        // when
        OccurredAt first = OccurredAt.of(value);
        OccurredAt second = OccurredAt.of(value);

        // then
        assertThat(first).isEqualTo(second);
    }
}

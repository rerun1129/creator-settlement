package com.creatorsettlement.domain.model.fee;

import com.creatorsettlement.domain.model.vo.FeeRate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeePolicy 도메인 단위 테스트")
class FeePolicyTest {

    @Test
    @DisplayName("모든 필드 정상이면 FeePolicy 인스턴스 반환")
    void of_returnsInstance_whenAllFieldsValid() {
        // given
        FeeRate rate = FeeRate.defaultRate();
        LocalDate effectiveFrom = LocalDate.of(2026, 1, 1);

        // when
        FeePolicy policy = FeePolicy.of(rate, effectiveFrom);

        // then
        assertThat(policy.rate()).isEqualTo(rate);
        assertThat(policy.effectiveFrom()).isEqualTo(effectiveFrom);
    }

    @Test
    @DisplayName("effectiveFrom이 null이면 예외")
    void of_throws_whenEffectiveFromIsNull() {
        // given
        FeeRate rate = FeeRate.defaultRate();

        // when & then
        assertThatThrownBy(() -> FeePolicy.of(rate, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

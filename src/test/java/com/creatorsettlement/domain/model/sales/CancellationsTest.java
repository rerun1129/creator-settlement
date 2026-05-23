package com.creatorsettlement.domain.model.sales;

import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cancellations 일급 컬렉션 단위 테스트")
class CancellationsTest {

    private static final SalesRecordId SALES_RECORD_ID = SalesRecordId.of(1L);
    private static final OccurredAt ANY_TIME = OccurredAt.of(LocalDateTime.of(2026, 5, 1, 10, 0));

    private CancellationRecord cancellationOf(String amount) {
        return CancellationRecord.of(SALES_RECORD_ID, Money.of(new BigDecimal(amount)), ANY_TIME);
    }

    @Test
    @DisplayName("취소 내역이 없으면 합계는 0이다")
    void total_returnsZero_whenEmpty() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of());

        // When
        Money result = cancellations.total();

        // Then
        assertThat(result.value()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("취소 내역이 하나면 합계는 그 금액이다")
    void total_returnsSum_whenSingle() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of(cancellationOf("3000")));

        // When
        Money result = cancellations.total();

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("취소 내역이 여럿이면 합계는 각 금액의 합이다")
    void total_returnsSum_whenMultiple() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of(cancellationOf("3000"), cancellationOf("5000")));

        // When
        Money result = cancellations.total();

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("8000"));
    }

    @Test
    @DisplayName("remainingOf는 결제 금액에서 누적 환불액을 뺀 값을 반환한다")
    void remainingOf_returnsPaymentMinusTotal() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of(cancellationOf("3000"), cancellationOf("2000")));
        Money payment = Money.of(new BigDecimal("10000"));

        // When
        Money result = cancellations.remainingOf(payment);

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("취소 내역이 없으면 remainingOf는 결제 금액 그대로 반환한다")
    void remainingOf_returnsPayment_whenEmpty() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of());
        Money payment = Money.of(new BigDecimal("10000"));

        // When
        Money result = cancellations.remainingOf(payment);

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("10000"));
    }

    @Test
    @DisplayName("누적 환불액이 결제 금액과 같으면 coversFully는 true이다")
    void coversFully_returnsTrue_whenTotalEqualsPayment() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of(cancellationOf("10000")));
        Money payment = Money.of(new BigDecimal("10000"));

        // When & Then
        assertThat(cancellations.coversFully(payment)).isTrue();
    }

    @Test
    @DisplayName("누적 환불액이 결제 금액을 초과하면 coversFully는 true이다")
    void coversFully_returnsTrue_whenTotalExceedsPayment() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of(cancellationOf("7000"), cancellationOf("5000")));
        Money payment = Money.of(new BigDecimal("10000"));

        // When & Then
        assertThat(cancellations.coversFully(payment)).isTrue();
    }

    @Test
    @DisplayName("누적 환불액이 결제 금액보다 적으면 coversFully는 false이다")
    void coversFully_returnsFalse_whenTotalLessThanPayment() {
        // Given
        Cancellations cancellations = Cancellations.of(List.of(cancellationOf("3000")));
        Money payment = Money.of(new BigDecimal("10000"));

        // When & Then
        assertThat(cancellations.coversFully(payment)).isFalse();
    }

    @Test
    @DisplayName("null 리스트로 생성 시 예외가 발생한다")
    void of_throws_whenNull() {
        // When & Then
        assertThatThrownBy(() -> Cancellations.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

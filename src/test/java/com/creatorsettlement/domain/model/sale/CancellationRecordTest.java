package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CancellationRecord 도메인 단위 테스트")
class CancellationRecordTest {

    @Test
    @DisplayName("정상 입력이면 취소 내역이 생성된다")
    void should_create_cancellation_record_when_input_is_valid() {
        // given
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        Money refundAmount = Money.of(new BigDecimal("10000"));
        OccurredAt cancelledAt = OccurredAt.of(LocalDateTime.now().minusHours(1));
        Money remainPaymentAmount = Money.of(new BigDecimal("10000"));

        // when
        CancellationRecord record = CancellationRecord.of(salesRecordId, refundAmount, cancelledAt, remainPaymentAmount);

        // then
        assertThat(record.getSalesRecordId()).isEqualTo(salesRecordId);
        assertThat(record.getRefundAmount()).isEqualTo(refundAmount);
        assertThat(record.getCancelledAt()).isEqualTo(cancelledAt);
        assertThat(record.getRemainPaymentAmount()).isEqualTo(remainPaymentAmount);
    }

    @Test
    @DisplayName("환불 금액이 잔여 환불 가능 금액을 초과하면 예외가 발생한다")
    void should_throw_when_refund_amount_exceeds_remaining() {
        // given
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        Money refundAmount = Money.of(new BigDecimal("10001"));
        OccurredAt cancelledAt = OccurredAt.of(LocalDateTime.now().minusHours(1));
        Money remainPaymentAmount = Money.of(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> CancellationRecord.of(salesRecordId, refundAmount, cancelledAt, remainPaymentAmount)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("환불 금액이 잔여 환불 가능 금액보다 작으면 부분 환불로 정상 생성된다")
    void should_create_cancellation_record_when_refund_is_partial() {
        // given
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        Money refundAmount = Money.of(new BigDecimal("3000"));
        OccurredAt cancelledAt = OccurredAt.of(LocalDateTime.now().minusHours(1));
        Money remainPaymentAmount = Money.of(new BigDecimal("10000"));

        // when
        CancellationRecord record = CancellationRecord.of(salesRecordId, refundAmount, cancelledAt, remainPaymentAmount);

        // then
        assertThat(record.getRefundAmount()).isEqualTo(refundAmount);
        assertThat(record.getRemainPaymentAmount()).isEqualTo(remainPaymentAmount);
    }

    @Test
    @DisplayName("환불 금액이 잔여 환불 가능 금액과 동일하면 전액 환불로 정상 생성된다")
    void should_create_cancellation_record_when_refund_equals_remaining() {
        // given
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        Money refundAmount = Money.of(new BigDecimal("10000"));
        OccurredAt cancelledAt = OccurredAt.of(LocalDateTime.now().minusHours(1));
        Money remainPaymentAmount = Money.of(new BigDecimal("10000"));

        // when & then
        assertThatNoException().isThrownBy(() -> CancellationRecord.of(salesRecordId, refundAmount, cancelledAt, remainPaymentAmount));
    }
}

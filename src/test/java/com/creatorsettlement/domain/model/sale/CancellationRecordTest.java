package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
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
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10000"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = originalPaidAt.plusMinutes(10);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when
        CancellationRecord record = new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, originalPaidAt);

        // then
        assertThat(record.getSalesRecordId()).isEqualTo(salesRecordId);
        assertThat(record.getRefundAmount()).isEqualTo(refundAmount);
        assertThat(record.getCancelledAt()).isEqualTo(cancelledAt);
        assertThat(record.getOriginalPaymentAmount()).isEqualTo(originalPaymentAmount);
        assertThat(record.getOriginalPaidAt()).isEqualTo(originalPaidAt);
    }

    @Test
    @DisplayName("환불 금액이 원본 결제 금액을 초과하면 예외가 발생한다")
    void should_throw_when_refund_amount_exceeds_original_payment() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10001"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = originalPaidAt.plusMinutes(10);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, originalPaidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("취소 일시가 원본 결제 일시보다 이전이면 예외가 발생한다")
    void should_throw_when_cancelled_at_is_before_original_paid_at() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10000"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = originalPaidAt.minusSeconds(1);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, originalPaidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("취소 일시가 원본 결제 일시와 동일하면 예외가 발생한다")
    void should_throw_when_cancelled_at_equals_original_paid_at() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10000"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = originalPaidAt;
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, originalPaidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("취소 일시가 미래이면 예외가 발생한다")
    void should_throw_when_cancelled_at_is_in_future() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10000"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = LocalDateTime.now().plusMinutes(1);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, originalPaidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("환불 금액이 null이면 예외가 발생한다")
    void should_throw_when_refund_amount_is_null() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = originalPaidAt.plusMinutes(10);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> new CancellationRecord(salesRecordId, null, cancelledAt, originalPaymentAmount, originalPaidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("취소 일시가 null이면 예외가 발생한다")
    void should_throw_when_cancelled_at_is_null() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10000"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> new CancellationRecord(salesRecordId, refundAmount, null, originalPaymentAmount, originalPaidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("원본 결제 일시가 null이면 예외가 발생한다")
    void should_throw_when_original_paid_at_is_null() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10000"));
        LocalDateTime cancelledAt = LocalDateTime.now().minusMinutes(30);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatThrownBy(() -> new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("환불 금액이 원본 결제 금액보다 작으면 부분 환불로 정상 생성된다")
    void should_create_cancellation_record_when_refund_is_partial() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("3000"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = originalPaidAt.plusMinutes(10);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when
        CancellationRecord record = new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, originalPaidAt);

        // then
        assertThat(record.getRefundAmount()).isEqualTo(refundAmount);
        assertThat(record.getOriginalPaymentAmount()).isEqualTo(originalPaymentAmount);
    }

    @Test
    @DisplayName("환불 금액이 원본 결제 금액과 동일하면 전액 환불로 정상 생성된다")
    void should_create_cancellation_record_when_refund_equals_original_payment() {
        // given
        SalesRecordId salesRecordId = new SalesRecordId(1L);
        Money refundAmount = new Money(new BigDecimal("10000"));
        LocalDateTime originalPaidAt = LocalDateTime.now().minusHours(1);
        LocalDateTime cancelledAt = originalPaidAt.plusMinutes(10);
        Money originalPaymentAmount = new Money(new BigDecimal("10000"));

        // when & then
        assertThatNoException().isThrownBy(() -> new CancellationRecord(salesRecordId, refundAmount, cancelledAt, originalPaymentAmount, originalPaidAt));
    }
}

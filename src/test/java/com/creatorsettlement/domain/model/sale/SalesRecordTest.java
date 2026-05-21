package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.StudentId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SalesRecord 도메인 단위 테스트")
class SalesRecordTest {

    @Test
    @DisplayName("정상 입력이면 판매 내역이 생성된다")
    void should_create_sales_record_when_input_is_valid() {
        // given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(10L);
        Money paymentAmount = Money.of(new BigDecimal("80000"));
        OccurredAt paidAt = OccurredAt.of(LocalDateTime.now().minusMinutes(1));

        // when
        SalesRecord record = SalesRecord.of(courseId, studentId, paymentAmount, paidAt);

        // then
        assertThat(record.getCourseId()).isEqualTo(courseId);
        assertThat(record.getStudentId()).isEqualTo(studentId);
        assertThat(record.getPaymentAmount()).isEqualTo(paymentAmount);
        assertThat(record.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    @DisplayName("환불 금액이 잔여 환불 가능 금액 이내면 통과한다")
    void should_pass_when_refund_within_remaining() {
        // given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(10L);
        Money paymentAmount = Money.of(new BigDecimal("10000"));
        OccurredAt paidAt = OccurredAt.of(LocalDateTime.now().minusMinutes(1));
        SalesRecord record = SalesRecord.of(courseId, studentId, paymentAmount, paidAt);
        Money cumulativeRefundedSoFar = Money.of(new BigDecimal("2000"));
        Money refundAmount = Money.of(new BigDecimal("5000"));

        // when & then
        assertThatNoException().isThrownBy(() -> record.validateRefund(refundAmount, cumulativeRefundedSoFar));
    }

    @Test
    @DisplayName("누적 환불이 0이고 환불이 결제 금액 이내면 통과한다")
    void should_pass_when_first_refund_with_zero_cumulative() {
        // given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(10L);
        Money paymentAmount = Money.of(new BigDecimal("10000"));
        OccurredAt paidAt = OccurredAt.of(LocalDateTime.now().minusMinutes(1));
        SalesRecord record = SalesRecord.of(courseId, studentId, paymentAmount, paidAt);
        Money cumulativeRefundedSoFar = Money.of(BigDecimal.ZERO);
        Money refundAmount = Money.of(new BigDecimal("5000"));

        // when & then
        assertThatNoException().isThrownBy(() -> record.validateRefund(refundAmount, cumulativeRefundedSoFar));
    }

    @Test
    @DisplayName("환불 금액이 잔여 환불 가능 금액과 같으면 통과한다")
    void should_pass_when_refund_equals_remaining() {
        // given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(10L);
        Money paymentAmount = Money.of(new BigDecimal("10000"));
        OccurredAt paidAt = OccurredAt.of(LocalDateTime.now().minusMinutes(1));
        SalesRecord record = SalesRecord.of(courseId, studentId, paymentAmount, paidAt);
        Money cumulativeRefundedSoFar = Money.of(new BigDecimal("3000"));
        Money refundAmount = Money.of(new BigDecimal("7000"));

        // when & then
        assertThatNoException().isThrownBy(() -> record.validateRefund(refundAmount, cumulativeRefundedSoFar));
    }

    @Test
    @DisplayName("환불 금액이 잔여 환불 가능 금액을 초과하면 예외가 발생한다")
    void should_throw_when_refund_exceeds_remaining() {
        // given
        CourseId courseId = CourseId.of(1L);
        StudentId studentId = StudentId.of(10L);
        Money paymentAmount = Money.of(new BigDecimal("10000"));
        OccurredAt paidAt = OccurredAt.of(LocalDateTime.now().minusMinutes(1));
        SalesRecord record = SalesRecord.of(courseId, studentId, paymentAmount, paidAt);
        Money cumulativeRefundedSoFar = Money.of(new BigDecimal("8000"));
        Money refundAmount = Money.of(new BigDecimal("3000"));

        // when & then
        assertThatThrownBy(() -> record.validateRefund(refundAmount, cumulativeRefundedSoFar))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
    }
}

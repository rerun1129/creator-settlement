package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
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
        CourseId courseId = new CourseId(1L);
        StudentId studentId = new StudentId(10L);
        Money paymentAmount = new Money(new BigDecimal("80000"));
        LocalDateTime paidAt = LocalDateTime.now().minusMinutes(1);

        // when
        SalesRecord record = new SalesRecord(courseId, studentId, paymentAmount, paidAt);

        // then
        assertThat(record.getCourseId()).isEqualTo(courseId);
        assertThat(record.getStudentId()).isEqualTo(studentId);
        assertThat(record.getPaymentAmount()).isEqualTo(paymentAmount);
        assertThat(record.getPaidAt()).isEqualTo(paidAt);
    }

    @Test
    @DisplayName("결제 일시가 미래이면 예외가 발생한다")
    void should_throw_when_paid_at_is_in_future() {
        // given
        CourseId courseId = new CourseId(1L);
        StudentId studentId = new StudentId(10L);
        Money paymentAmount = new Money(new BigDecimal("10000"));
        LocalDateTime futurePaidAt = LocalDateTime.now().plusMinutes(1);

        // when & then
        assertThatThrownBy(() -> new SalesRecord(courseId, studentId, paymentAmount, futurePaidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("결제 금액이 null이면 예외가 발생한다")
    void should_throw_when_payment_amount_is_null() {
        // given
        CourseId courseId = new CourseId(1L);
        StudentId studentId = new StudentId(10L);
        LocalDateTime paidAt = LocalDateTime.now().minusMinutes(1);

        // when & then
        assertThatThrownBy(() -> new SalesRecord(courseId, studentId, null, paidAt)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("결제 일시가 null이면 예외가 발생한다")
    void should_throw_when_paid_at_is_null() {
        // given
        CourseId courseId = new CourseId(1L);
        StudentId studentId = new StudentId(10L);
        Money paymentAmount = new Money(new BigDecimal("80000"));

        // when & then
        assertThatThrownBy(() -> new SalesRecord(courseId, studentId, paymentAmount, null)).isInstanceOf(IllegalArgumentException.class);
    }
}

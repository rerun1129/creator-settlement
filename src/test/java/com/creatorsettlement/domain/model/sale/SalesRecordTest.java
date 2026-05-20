package com.creatorsettlement.domain.model.sale;

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
        SalesRecord record = new SalesRecord(courseId, studentId, paymentAmount, paidAt);

        // then
        assertThat(record.getCourseId()).isEqualTo(courseId);
        assertThat(record.getStudentId()).isEqualTo(studentId);
        assertThat(record.getPaymentAmount()).isEqualTo(paymentAmount);
        assertThat(record.getPaidAt()).isEqualTo(paidAt);
    }
}

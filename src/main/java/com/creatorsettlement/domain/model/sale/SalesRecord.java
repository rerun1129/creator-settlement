package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
import java.time.LocalDateTime;

public class SalesRecord {

    private final Long courseId;
    private final Long studentId;
    private final Money paymentAmount;
    private final LocalDateTime paidAt;

    public SalesRecord(Long courseId, Long studentId, Money paymentAmount, LocalDateTime paidAt) {
        if (courseId == null) {
            throw new IllegalArgumentException("Course ID는 null일 수 없습니다");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID는 null일 수 없습니다");
        }
        if (paymentAmount == null) {
            throw new IllegalArgumentException("결제 금액은 null일 수 없습니다");
        }
        if (paidAt == null) {
            throw new IllegalArgumentException("결제 일시는 null일 수 없습니다");
        }
        if (paidAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("결제 일시는 미래일 수 없습니다");
        }
        this.courseId = courseId;
        this.studentId = studentId;
        this.paymentAmount = paymentAmount;
        this.paidAt = paidAt;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Money getPaymentAmount() {
        return paymentAmount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}

package com.creatorsettlement.domain.model.sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SalesRecord {

    private final Long courseId;
    private final Long studentId;
    private final BigDecimal paymentAmount;
    private final LocalDateTime paidAt;

    public SalesRecord(Long courseId, Long studentId, BigDecimal paymentAmount, LocalDateTime paidAt) {
        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
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

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}

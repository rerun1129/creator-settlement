package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
import java.time.LocalDateTime;

public class SalesRecord {

    private final Long courseId;
    private final Long studentId;
    private final Money paymentAmount;
    private final LocalDateTime paidAt;

    public SalesRecord(Long courseId, Long studentId, Money paymentAmount, LocalDateTime paidAt) {
        // Money 생성 시점에 양수 invariant가 이미 보장됨
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

package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.StudentId;

import java.math.BigDecimal;

public class SalesRecord {

    private final CourseId courseId;
    private final StudentId studentId;
    private final Money paymentAmount;
    private final OccurredAt paidAt;

    public static SalesRecord of(CourseId courseId, StudentId studentId, Money paymentAmount, OccurredAt paidAt) {
        return new SalesRecord(courseId, studentId, paymentAmount, paidAt);
    }

    private SalesRecord(CourseId courseId, StudentId studentId, Money paymentAmount, OccurredAt paidAt) {
        this.courseId = courseId;
        this.studentId = studentId;
        this.paymentAmount = paymentAmount;
        this.paidAt = paidAt;
    }

    public CourseId getCourseId() {
        return courseId;
    }

    public StudentId getStudentId() {
        return studentId;
    }

    public Money getPaymentAmount() {
        return paymentAmount;
    }

    public OccurredAt getPaidAt() {
        return paidAt;
    }

    public void validateRefund(Money refundAmount, Money cumulativeRefundedSoFar) {
        BigDecimal remain = this.paymentAmount.value().subtract(cumulativeRefundedSoFar.value());
        if (refundAmount.value().compareTo(remain) > 0) {
            throw new IllegalArgumentException(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
        }
    }
}

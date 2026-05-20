package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.StudentId;
import java.time.LocalDateTime;

public class SalesRecord {

    private final CourseId courseId;
    private final StudentId studentId;
    private final Money paymentAmount;
    private final LocalDateTime paidAt;

    public SalesRecord(CourseId courseId, StudentId studentId, Money paymentAmount, LocalDateTime paidAt) {
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

    public CourseId getCourseId() {
        return courseId;
    }

    public StudentId getStudentId() {
        return studentId;
    }

    public Money getPaymentAmount() {
        return paymentAmount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}

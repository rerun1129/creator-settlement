package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.StudentId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegisterSaleCommand(
        Long courseId,
        Long studentId,
        BigDecimal paymentAmount,
        LocalDateTime paidAt
) {
    public SalesRecord toSalesRecord() {
        return SalesRecord.of(
                CourseId.of(courseId),
                StudentId.of(studentId),
                Money.of(paymentAmount),
                OccurredAt.of(paidAt)
        );
    }
}

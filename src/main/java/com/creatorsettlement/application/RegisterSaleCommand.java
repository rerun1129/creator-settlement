package com.creatorsettlement.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegisterSaleCommand(
        Long courseId,
        Long studentId,
        BigDecimal paymentAmount,
        LocalDateTime paidAt
) {
}

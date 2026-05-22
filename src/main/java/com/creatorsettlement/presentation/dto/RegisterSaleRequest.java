package com.creatorsettlement.presentation.dto;

import com.creatorsettlement.application.RegisterSaleCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegisterSaleRequest(
        @NotNull Long courseId,
        @NotNull Long studentId,
        @NotNull @PositiveOrZero BigDecimal paymentAmount,
        @NotNull LocalDateTime paidAt
) {
    public RegisterSaleCommand toCommand() {
        return new RegisterSaleCommand(courseId, studentId, paymentAmount, paidAt);
    }
}

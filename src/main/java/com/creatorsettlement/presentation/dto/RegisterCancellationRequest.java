package com.creatorsettlement.presentation.dto;

import com.creatorsettlement.application.RegisterCancellationCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegisterCancellationRequest(
        @NotNull Long salesRecordId,
        @NotNull @PositiveOrZero BigDecimal refundAmount,
        @NotNull LocalDateTime cancelledAt
) {
    public RegisterCancellationCommand toCommand() {
        return new RegisterCancellationCommand(salesRecordId, refundAmount, cancelledAt);
    }
}

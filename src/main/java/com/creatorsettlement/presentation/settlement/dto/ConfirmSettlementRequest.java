package com.creatorsettlement.presentation.settlement.dto;

import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.YearMonth;

public record ConfirmSettlementRequest(
        @NotNull Long creatorId,
        @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
        @NotNull LocalDateTime confirmedAt
) {
    public ConfirmSettlementCommand toCommand() {
        return new ConfirmSettlementCommand(creatorId, yearMonth, confirmedAt);
    }
}

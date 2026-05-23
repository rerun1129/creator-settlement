package com.creatorsettlement.presentation.settlement.dto;

import com.creatorsettlement.application.settlement.dto.PaySettlementCommand;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.YearMonth;

public record PaySettlementRequest(
        @NotNull Long creatorId,
        @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth,
        @NotNull LocalDateTime paidAt
) {
    public PaySettlementCommand toCommand() {
        return new PaySettlementCommand(creatorId, yearMonth, paidAt);
    }
}

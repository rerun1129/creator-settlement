package com.creatorsettlement.presentation.settlement.dto;

import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.YearMonth;

public record GetMonthlySettlementRequest(
        @NotNull Long creatorId,
        @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth
) {
    public MonthlySettlementQuery toQuery() {
        return new MonthlySettlementQuery(creatorId, yearMonth);
    }
}

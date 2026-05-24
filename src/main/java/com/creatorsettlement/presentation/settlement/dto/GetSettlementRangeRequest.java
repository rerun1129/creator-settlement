package com.creatorsettlement.presentation.settlement.dto;

import com.creatorsettlement.application.settlement.dto.SettlementRangeQuery;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record GetSettlementRangeRequest(
        @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
        @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to
) {
    public SettlementRangeQuery toQuery() {
        return new SettlementRangeQuery(from, to);
    }

    @AssertTrue
    @SuppressWarnings("unused")
    public boolean isFromNotAfterTo() {
        return from == null || to == null || !from.isAfter(to);
    }
}

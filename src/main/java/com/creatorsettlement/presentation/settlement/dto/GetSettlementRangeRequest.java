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
    @AssertTrue(message = "조회 시작 일자가 종료 일자보다 늦을 수 없습니다")
    public boolean isFromBeforeOrEqualTo() {
        if (from == null || to == null) {
            return true;
        }
        return !from.isAfter(to);
    }

    public SettlementRangeQuery toQuery() {
        return new SettlementRangeQuery(from, to);
    }
}

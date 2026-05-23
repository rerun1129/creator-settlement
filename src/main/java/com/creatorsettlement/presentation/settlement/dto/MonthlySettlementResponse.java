package com.creatorsettlement.presentation.settlement.dto;

import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
import com.creatorsettlement.domain.model.settlement.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record MonthlySettlementResponse(
        Long creatorId,
        YearMonth yearMonth,
        SettlementStatus status,
        BigDecimal totalSales,
        BigDecimal totalRefund,
        BigDecimal netSales,
        BigDecimal feeRate,
        BigDecimal platformFee,
        BigDecimal expectedPayout,
        long salesCount,
        long cancellationCount,
        LocalDateTime confirmedAt
) {
    public static MonthlySettlementResponse from(MonthlySettlementView view) {
        return new MonthlySettlementResponse(
                view.creatorId(),
                view.yearMonth(),
                view.status(),
                view.totalSales(),
                view.totalRefund(),
                view.netSales(),
                view.feeRate(),
                view.platformFee(),
                view.expectedPayout(),
                view.salesCount(),
                view.cancellationCount(),
                view.confirmedAt()
        );
    }
}

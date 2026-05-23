package com.creatorsettlement.application.settlement.dto;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.settlement.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record MonthlySettlementView(
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
        LocalDateTime confirmedAt,
        LocalDateTime paidAt
) {

    public static MonthlySettlementView from(Settlement s) {
        return new MonthlySettlementView(
                s.creatorId().value(),
                s.yearMonth(),
                s.status(),
                s.totalSales().value(),
                s.totalRefund().value(),
                s.netSales().value(),
                s.feeRate().value(),
                s.platformFee().value(),
                s.expectedPayout().value(),
                s.salesCount(),
                s.cancellationCount(),
                s.confirmedAt() == null ? null : s.confirmedAt().value(),
                s.paidAt() == null ? null : s.paidAt().value()
        );
    }
}

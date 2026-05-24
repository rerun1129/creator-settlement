package com.creatorsettlement.domain.model.settlement;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SettlementAmount;

import java.time.YearMonth;

public enum SettlementStatus {
    PENDING {
        @Override public SettlementStatus toConfirmed() { return CONFIRMED; }
        @Override public SettlementStatus toPaid() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_NOT_CONFIRMED_FOR_PAYMENT.message());
        }
        @Override public Settlement toSettlement(
                CreatorId creatorId, YearMonth yearMonth,
                Money totalSales, Money totalRefund, SettlementAmount netSales,
                FeeRate feeRate, Money platformFee, SettlementAmount expectedPayout,
                long salesCount, long cancellationCount,
                OccurredAt confirmedAt, OccurredAt paidAt
                                                ) {
            return Settlement.pendingSnapshot(
                    creatorId, yearMonth, totalSales, totalRefund, netSales,
                    feeRate, platformFee, expectedPayout,
                    salesCount, cancellationCount
            );
        }
    },
    CONFIRMED {
        @Override public SettlementStatus toConfirmed() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_CONFIRMED.message());
        }
        @Override public SettlementStatus toPaid() { return PAID; }
        @Override public Settlement toSettlement(
                CreatorId creatorId, YearMonth yearMonth,
                Money totalSales, Money totalRefund, SettlementAmount netSales,
                FeeRate feeRate, Money platformFee, SettlementAmount expectedPayout,
                long salesCount, long cancellationCount,
                OccurredAt confirmedAt, OccurredAt paidAt
                                                ) {
            return Settlement.confirmedSnapshot(
                    creatorId, yearMonth, totalSales, totalRefund, netSales,
                    feeRate, platformFee, expectedPayout,
                    salesCount, cancellationCount, confirmedAt
            );
        }
    },
    PAID {
        @Override public SettlementStatus toConfirmed() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_PAID.message());
        }
        @Override public SettlementStatus toPaid() {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_PAID.message());
        }
        @Override public Settlement toSettlement(
                CreatorId creatorId, YearMonth yearMonth,
                Money totalSales, Money totalRefund, SettlementAmount netSales,
                FeeRate feeRate, Money platformFee, SettlementAmount expectedPayout,
                long salesCount, long cancellationCount,
                OccurredAt confirmedAt, OccurredAt paidAt
                                                ) {
            return Settlement.paidSnapshot(
                    creatorId, yearMonth, totalSales, totalRefund, netSales,
                    feeRate, platformFee, expectedPayout,
                    salesCount, cancellationCount, confirmedAt, paidAt
            );
        }
    };

    public abstract SettlementStatus toConfirmed();
    public abstract SettlementStatus toPaid();
    public abstract Settlement toSettlement(
            CreatorId creatorId, YearMonth yearMonth,
            Money totalSales, Money totalRefund, SettlementAmount netSales,
            FeeRate feeRate, Money platformFee, SettlementAmount expectedPayout,
            long salesCount, long cancellationCount,
            OccurredAt confirmedAt, OccurredAt paidAt
                                           );
}

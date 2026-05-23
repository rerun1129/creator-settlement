package com.creatorsettlement.domain.model.settlement;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SettlementAmount;

import java.time.YearMonth;

public class Settlement {

    private final CreatorId creatorId;
    private final YearMonth yearMonth;
    private SettlementStatus status;
    private final Money totalSales;
    private final Money totalRefund;
    private final SettlementAmount netSales;
    private final FeeRate feeRate;
    private final Money platformFee;
    private final SettlementAmount expectedPayout;
    private final long salesCount;
    private final long cancellationCount;
    private OccurredAt confirmedAt;
    private OccurredAt paidAt;

    private Settlement(
            CreatorId creatorId,
            YearMonth yearMonth,
            SettlementStatus status,
            Money totalSales,
            Money totalRefund,
            SettlementAmount netSales,
            FeeRate feeRate,
            Money platformFee,
            SettlementAmount expectedPayout,
            long salesCount,
            long cancellationCount,
            OccurredAt confirmedAt,
            OccurredAt paidAt
    ) {
        this.creatorId = creatorId;
        this.yearMonth = yearMonth;
        this.status = status;
        this.totalSales = totalSales;
        this.totalRefund = totalRefund;
        this.netSales = netSales;
        this.feeRate = feeRate;
        this.platformFee = platformFee;
        this.expectedPayout = expectedPayout;
        this.salesCount = salesCount;
        this.cancellationCount = cancellationCount;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
    }

    public static Settlement pendingSnapshot(
            CreatorId creatorId, YearMonth yearMonth,
            Money totalSales, Money totalRefund, SettlementAmount netSales,
            FeeRate feeRate, Money platformFee, SettlementAmount expectedPayout,
            long salesCount, long cancellationCount
    ) {
        return new Settlement(
                creatorId, yearMonth, SettlementStatus.PENDING,
                totalSales, totalRefund, netSales,
                feeRate, platformFee, expectedPayout,
                salesCount, cancellationCount, null, null
        );
    }

    public static Settlement confirmedSnapshot(
            CreatorId creatorId, YearMonth yearMonth,
            Money totalSales, Money totalRefund, SettlementAmount netSales,
            FeeRate feeRate, Money platformFee, SettlementAmount expectedPayout,
            long salesCount, long cancellationCount,
            OccurredAt confirmedAt
    ) {
        return new Settlement(
                creatorId, yearMonth, SettlementStatus.CONFIRMED,
                totalSales, totalRefund, netSales,
                feeRate, platformFee, expectedPayout,
                salesCount, cancellationCount, confirmedAt, null
        );
    }

    public static Settlement paidSnapshot(
            CreatorId creatorId, YearMonth yearMonth,
            Money totalSales, Money totalRefund, SettlementAmount netSales,
            FeeRate feeRate, Money platformFee, SettlementAmount expectedPayout,
            long salesCount, long cancellationCount,
            OccurredAt confirmedAt, OccurredAt paidAt
    ) {
        return new Settlement(
                creatorId, yearMonth, SettlementStatus.PAID,
                totalSales, totalRefund, netSales,
                feeRate, platformFee, expectedPayout,
                salesCount, cancellationCount, confirmedAt, paidAt
        );
    }

    public CreatorId creatorId() { return creatorId; }
    public YearMonth yearMonth() { return yearMonth; }
    public SettlementStatus status() { return status; }
    public Money totalSales() { return totalSales; }
    public Money totalRefund() { return totalRefund; }
    public SettlementAmount netSales() { return netSales; }
    public FeeRate feeRate() { return feeRate; }
    public Money platformFee() { return platformFee; }
    public SettlementAmount expectedPayout() { return expectedPayout; }
    public long salesCount() { return salesCount; }
    public long cancellationCount() { return cancellationCount; }
    public OccurredAt confirmedAt() { return confirmedAt; }
    public OccurredAt paidAt() { return paidAt; }

    public void confirm(OccurredAt occurredAt) {
        switch (status) {
            case PENDING -> { status = SettlementStatus.CONFIRMED; confirmedAt = occurredAt; }
            case CONFIRMED -> throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_CONFIRMED.message());
            case PAID -> throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_PAID.message());
        }
    }

    public void pay(OccurredAt occurredAt) {
        switch (status) {
            case CONFIRMED -> { status = SettlementStatus.PAID; paidAt = occurredAt; }
            case PENDING -> throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_NOT_CONFIRMED_FOR_PAYMENT.message());
            case PAID -> throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_ALREADY_PAID.message());
        }
    }
}

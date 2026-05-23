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
            OccurredAt confirmedAt
    ) {
        if (creatorId == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_CREATOR_ID_NULL.message());
        if (yearMonth == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_YEAR_MONTH_NULL.message());
        if (status == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_STATUS_NULL.message());
        if (totalSales == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_TOTAL_SALES_NULL.message());
        if (totalRefund == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_TOTAL_REFUND_NULL.message());
        if (netSales == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_NET_SALES_NULL.message());
        if (feeRate == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_FEE_RATE_NULL.message());
        if (platformFee == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_PLATFORM_FEE_NULL.message());
        if (expectedPayout == null) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_EXPECTED_PAYOUT_NULL.message());
        if (salesCount < 0) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_SALES_COUNT_NEGATIVE.message());
        if (cancellationCount < 0) throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_CANCELLATION_COUNT_NEGATIVE.message());

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
                salesCount, cancellationCount, null
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
}

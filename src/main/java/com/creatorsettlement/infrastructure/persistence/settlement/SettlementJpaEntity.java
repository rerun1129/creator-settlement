package com.creatorsettlement.infrastructure.persistence.settlement;

import com.creatorsettlement.domain.model.settlement.SettlementStatus;
import com.creatorsettlement.infrastructure.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "settlement",
    uniqueConstraints = @UniqueConstraint(name = "uk_settlement_creator_target_month", columnNames = {"creator_id", "target_month"})
)
public class SettlementJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "settlement_id")
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "target_month", nullable = false, length = 6)
    private String yearMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "total_sales", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "total_refund", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalRefund;

    @Column(name = "net_sales", nullable = false, precision = 19, scale = 2)
    private BigDecimal netSales;

    @Column(name = "fee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal feeRate;

    @Column(name = "platform_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "expected_payout", nullable = false, precision = 19, scale = 2)
    private BigDecimal expectedPayout;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "cancellation_count", nullable = false)
    private long cancellationCount;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    protected SettlementJpaEntity() {}

    static SettlementJpaEntity of(
            Long creatorId, String yearMonth, SettlementStatus status,
            BigDecimal totalSales, BigDecimal totalRefund, BigDecimal netSales,
            BigDecimal feeRate, BigDecimal platformFee, BigDecimal expectedPayout,
            long salesCount, long cancellationCount,
            LocalDateTime confirmedAt, LocalDateTime paidAt
    ) {
        SettlementJpaEntity e = new SettlementJpaEntity();
        e.creatorId = creatorId;
        e.yearMonth = yearMonth;
        e.status = status;
        e.totalSales = totalSales;
        e.totalRefund = totalRefund;
        e.netSales = netSales;
        e.feeRate = feeRate;
        e.platformFee = platformFee;
        e.expectedPayout = expectedPayout;
        e.salesCount = salesCount;
        e.cancellationCount = cancellationCount;
        e.confirmedAt = confirmedAt;
        e.paidAt = paidAt;
        return e;
    }

    void applyStateTransition(SettlementStatus status, LocalDateTime confirmedAt, LocalDateTime paidAt) {
        this.status = status;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
    }

    Long getId() { return id; }
    Long getCreatorId() { return creatorId; }
    String getYearMonth() { return yearMonth; }
    SettlementStatus getStatus() { return status; }
    BigDecimal getTotalSales() { return totalSales; }
    BigDecimal getTotalRefund() { return totalRefund; }
    BigDecimal getNetSales() { return netSales; }
    BigDecimal getFeeRate() { return feeRate; }
    BigDecimal getPlatformFee() { return platformFee; }
    BigDecimal getExpectedPayout() { return expectedPayout; }
    long getSalesCount() { return salesCount; }
    long getCancellationCount() { return cancellationCount; }
    LocalDateTime getConfirmedAt() { return confirmedAt; }
    LocalDateTime getPaidAt() { return paidAt; }
}

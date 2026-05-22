package com.creatorsettlement.infrastructure.persistence.sales;

import com.creatorsettlement.infrastructure.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cancellation_record")
class CancellationRecordJpaEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cancellation_record_id")
    private Long id;

    @Column(name = "sales_record_id", nullable = false)
    private Long salesRecordId;

    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "cancelled_at", nullable = false)
    private LocalDateTime cancelledAt;

    protected CancellationRecordJpaEntity() {
    }

    static CancellationRecordJpaEntity of(Long salesRecordId, BigDecimal refundAmount, LocalDateTime cancelledAt) {
        CancellationRecordJpaEntity entity = new CancellationRecordJpaEntity();
        entity.salesRecordId = salesRecordId;
        entity.refundAmount = refundAmount;
        entity.cancelledAt = cancelledAt;
        return entity;
    }

    Long getId() {
        return id;
    }

    Long getSalesRecordId() {
        return salesRecordId;
    }

    BigDecimal getRefundAmount() {
        return refundAmount;
    }

    LocalDateTime getCancelledAt() {
        return cancelledAt;
    }
}

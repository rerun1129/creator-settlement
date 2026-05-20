package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import java.time.LocalDateTime;

public class CancellationRecord {

    private final SalesRecordId salesRecordId;
    private final Money refundAmount;
    private final LocalDateTime cancelledAt;
    private final Money originalPaymentAmount;
    private final LocalDateTime originalPaidAt;

    public CancellationRecord(SalesRecordId salesRecordId, Money refundAmount, LocalDateTime cancelledAt, Money originalPaymentAmount, LocalDateTime originalPaidAt) {
        if (cancelledAt == null) {
            throw new IllegalArgumentException("취소 일시는 null일 수 없습니다");
        }
        if (originalPaidAt == null) {
            throw new IllegalArgumentException("원본 결제 일시는 null일 수 없습니다");
        }
        if (refundAmount.value().compareTo(originalPaymentAmount.value()) > 0) {
            throw new IllegalArgumentException("환불 금액은 원본 결제 금액을 초과할 수 없습니다");
        }
        if (!cancelledAt.isAfter(originalPaidAt)) {
            throw new IllegalArgumentException("취소 일시는 원본 결제 일시 이후여야 합니다");
        }
        if (cancelledAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("취소 일시는 미래일 수 없습니다");
        }
        this.salesRecordId = salesRecordId;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
        this.originalPaymentAmount = originalPaymentAmount;
        this.originalPaidAt = originalPaidAt;
    }

    public SalesRecordId getSalesRecordId() {
        return salesRecordId;
    }

    public Money getRefundAmount() {
        return refundAmount;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public Money getOriginalPaymentAmount() {
        return originalPaymentAmount;
    }

    public LocalDateTime getOriginalPaidAt() {
        return originalPaidAt;
    }
}

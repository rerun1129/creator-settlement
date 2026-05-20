package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

public class CancellationRecord {

    private final SalesRecordId salesRecordId;
    private final Money refundAmount;
    private final OccurredAt cancelledAt;
    private final Money originalPaymentAmount;
    private final OccurredAt originalPaidAt;

    public CancellationRecord(SalesRecordId salesRecordId, Money refundAmount, OccurredAt cancelledAt, Money originalPaymentAmount, OccurredAt originalPaidAt) {
        if (refundAmount.value().compareTo(originalPaymentAmount.value()) > 0) {
            throw new IllegalArgumentException("환불 금액은 원본 결제 금액을 초과할 수 없습니다");
        }
        if (!cancelledAt.value().isAfter(originalPaidAt.value())) {
            throw new IllegalArgumentException("취소 일시는 원본 결제 일시 이후여야 합니다");
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

    public OccurredAt getCancelledAt() {
        return cancelledAt;
    }

    public Money getOriginalPaymentAmount() {
        return originalPaymentAmount;
    }

    public OccurredAt getOriginalPaidAt() {
        return originalPaidAt;
    }
}

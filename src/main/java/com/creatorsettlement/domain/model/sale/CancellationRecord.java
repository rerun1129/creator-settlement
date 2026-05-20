package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

public class CancellationRecord {

    private final SalesRecordId salesRecordId;
    private final Money refundAmount;
    private final OccurredAt cancelledAt;
    private final Money remainPaymentAmount;
    private final OccurredAt originalPaidAt;

    public static CancellationRecord of(SalesRecordId salesRecordId, Money refundAmount, OccurredAt cancelledAt, Money remainPaymentAmount, OccurredAt originalPaidAt) {
        return new CancellationRecord(salesRecordId, refundAmount, cancelledAt, remainPaymentAmount, originalPaidAt);
    }

    private CancellationRecord(SalesRecordId salesRecordId, Money refundAmount, OccurredAt cancelledAt, Money remainPaymentAmount, OccurredAt originalPaidAt) {
        if (refundAmount.value().compareTo(remainPaymentAmount.value()) > 0) {
            throw new IllegalArgumentException(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
        }
        if (!cancelledAt.value().isAfter(originalPaidAt.value())) {
            throw new IllegalArgumentException(DomainErrorMessage.CANCELLED_AT_NOT_AFTER_ORIGINAL_PAID_AT.message());
        }
        this.salesRecordId = salesRecordId;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
        this.remainPaymentAmount = remainPaymentAmount;
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

    public Money getRemainPaymentAmount() {
        return remainPaymentAmount;
    }

    public OccurredAt getOriginalPaidAt() {
        return originalPaidAt;
    }
}

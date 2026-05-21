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

    public static CancellationRecord of(SalesRecordId salesRecordId, Money refundAmount, OccurredAt cancelledAt, Money remainPaymentAmount) {
        return new CancellationRecord(salesRecordId, refundAmount, cancelledAt, remainPaymentAmount);
    }

    private CancellationRecord(SalesRecordId salesRecordId, Money refundAmount, OccurredAt cancelledAt, Money remainPaymentAmount) {
        if (refundAmount.value().compareTo(remainPaymentAmount.value()) > 0) {
            throw new IllegalArgumentException(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
        }
        this.salesRecordId = salesRecordId;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
        this.remainPaymentAmount = remainPaymentAmount;
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
}

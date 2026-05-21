package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

public class CancellationRecord {

    private final SalesRecordId salesRecordId;
    private final Money refundAmount;
    private final OccurredAt cancelledAt;

    public static CancellationRecord of(SalesRecordId salesRecordId, Money refundAmount, OccurredAt cancelledAt) {
        return new CancellationRecord(salesRecordId, refundAmount, cancelledAt);
    }

    private CancellationRecord(SalesRecordId salesRecordId, Money refundAmount, OccurredAt cancelledAt) {
        this.salesRecordId = salesRecordId;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
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
}

package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;

public record CancellationView(Money refundAmount, OccurredAt cancelledAt) {
    public static CancellationView from(CancellationRecord record) {
        return new CancellationView(record.getRefundAmount(), record.getCancelledAt());
    }
}

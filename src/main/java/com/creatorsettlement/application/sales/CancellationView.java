package com.creatorsettlement.application.sales;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;

public record CancellationView(Money refundAmount, OccurredAt cancelledAt) {
    public static CancellationView from(CancellationRecord record) {
        return new CancellationView(record.getRefundAmount(), record.getCancelledAt());
    }
}

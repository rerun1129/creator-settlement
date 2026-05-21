package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegisterCancellationCommand(
        Long salesRecordId,
        BigDecimal refundAmount,
        LocalDateTime cancelledAt
) {
    public CancellationRecord toCancellationRecord() {
        return CancellationRecord.of(
                SalesRecordId.of(salesRecordId),
                Money.of(refundAmount),
                OccurredAt.of(cancelledAt)
        );
    }
}

package com.creatorsettlement.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RegisterCancellationCommand(
        Long salesRecordId,
        BigDecimal refundAmount,
        LocalDateTime cancelledAt
) {
}

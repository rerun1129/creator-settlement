package com.creatorsettlement.presentation.sales.dto;

import com.creatorsettlement.application.sales.dto.CancellationView;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CancellationResponse(BigDecimal refundAmount, LocalDateTime cancelledAt) {
    public static CancellationResponse from(CancellationView view) {
        return new CancellationResponse(
                view.refundAmount().value(),
                view.cancelledAt().value()
        );
    }
}

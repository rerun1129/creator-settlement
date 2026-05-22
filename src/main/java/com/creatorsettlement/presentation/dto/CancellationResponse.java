package com.creatorsettlement.presentation.dto;

import com.creatorsettlement.application.CancellationView;
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

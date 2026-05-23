package com.creatorsettlement.presentation.settlement.dto;

import com.creatorsettlement.application.settlement.dto.SettlementRangeView;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record SettlementRangeResponse(List<CreatorPayableResponse> responses, BigDecimal totalAmount) {
    public static SettlementRangeResponse from(SettlementRangeView view) {
        List<CreatorPayableResponse> sorted = view.responses().stream()
                .map(CreatorPayableResponse::from)
                .sorted(Comparator.comparing(CreatorPayableResponse::creatorId))
                .toList();
        return new SettlementRangeResponse(sorted, view.totalAmount());
    }
}

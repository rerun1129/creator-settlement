package com.creatorsettlement.presentation.settlement.dto;

import com.creatorsettlement.application.settlement.dto.CreatorPayableView;

import java.math.BigDecimal;

public record CreatorPayableResponse(Long creatorId, BigDecimal expectedSettlementAmount) {
    public static CreatorPayableResponse from(CreatorPayableView view) {
        return new CreatorPayableResponse(view.creatorId(), view.expectedSettlementAmount());
    }
}

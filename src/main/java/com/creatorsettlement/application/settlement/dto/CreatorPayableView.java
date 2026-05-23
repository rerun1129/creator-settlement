package com.creatorsettlement.application.settlement.dto;

import java.math.BigDecimal;

public record CreatorPayableView(Long creatorId, BigDecimal expectedSettlementAmount) {
}

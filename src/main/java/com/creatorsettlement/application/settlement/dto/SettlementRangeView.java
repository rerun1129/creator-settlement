package com.creatorsettlement.application.settlement.dto;

import java.math.BigDecimal;
import java.util.List;

public record SettlementRangeView(List<CreatorPayableView> responses, BigDecimal totalAmount) {
}

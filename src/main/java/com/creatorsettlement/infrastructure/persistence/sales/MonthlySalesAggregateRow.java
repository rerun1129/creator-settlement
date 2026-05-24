package com.creatorsettlement.infrastructure.persistence.sales;

import java.math.BigDecimal;

public record MonthlySalesAggregateRow(Long creatorId, Integer year, Integer month, BigDecimal totalAmount) {
}

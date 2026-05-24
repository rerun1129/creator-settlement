package com.creatorsettlement.infrastructure.persistence.sales;

import java.math.BigDecimal;

public record MonthlyCancellationAggregateRow(Long creatorId, Integer year, Integer month, BigDecimal totalRefund) {
}

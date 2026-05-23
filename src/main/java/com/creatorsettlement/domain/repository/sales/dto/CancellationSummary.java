package com.creatorsettlement.domain.repository.sales.dto;

import com.creatorsettlement.domain.model.vo.Money;

public record CancellationSummary(Money totalRefund, long count) {
}

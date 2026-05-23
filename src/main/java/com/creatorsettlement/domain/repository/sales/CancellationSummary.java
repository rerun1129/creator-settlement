package com.creatorsettlement.domain.repository.sales;

import com.creatorsettlement.domain.model.vo.Money;

public record CancellationSummary(Money totalRefund, long count) {
}

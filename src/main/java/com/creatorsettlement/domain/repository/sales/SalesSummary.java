package com.creatorsettlement.domain.repository.sales;

import com.creatorsettlement.domain.model.vo.Money;

public record SalesSummary(Money totalAmount, long count) {
}

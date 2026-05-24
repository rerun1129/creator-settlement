package com.creatorsettlement.domain.repository.sales.dto;

import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;

import java.time.YearMonth;

public record MonthlyCancellationAggregate(CreatorId creatorId, YearMonth yearMonth, Money totalRefund) {
}

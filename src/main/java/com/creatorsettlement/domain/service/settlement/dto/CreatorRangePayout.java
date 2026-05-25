package com.creatorsettlement.domain.service.settlement.dto;

import com.creatorsettlement.domain.model.vo.CreatorId;

import java.math.BigDecimal;

public record CreatorRangePayout(CreatorId creatorId, BigDecimal expectedPayout) {
}

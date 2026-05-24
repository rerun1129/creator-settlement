package com.creatorsettlement.domain.service.settlement;

import java.math.BigDecimal;
import java.util.List;

public record SettlementRangePayoutResult(List<CreatorRangePayout> payouts, BigDecimal totalAmount) {
}

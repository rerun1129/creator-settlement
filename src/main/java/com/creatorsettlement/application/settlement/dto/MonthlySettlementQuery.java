package com.creatorsettlement.application.settlement.dto;

import java.time.YearMonth;

public record MonthlySettlementQuery(Long creatorId, YearMonth yearMonth) {
}

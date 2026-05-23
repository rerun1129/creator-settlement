package com.creatorsettlement.application.settlement;

import java.time.YearMonth;

public record MonthlySettlementQuery(Long creatorId, YearMonth yearMonth) {
}

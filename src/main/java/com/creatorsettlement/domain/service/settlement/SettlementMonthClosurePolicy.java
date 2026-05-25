package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.error.DomainErrorMessage;

import java.time.YearMonth;
import java.time.ZoneId;

public class SettlementMonthClosurePolicy {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    public void verifyMonthClosed(YearMonth yearMonth) {
        YearMonth current = YearMonth.now(KST_ZONE);
        if (!yearMonth.isBefore(current)) {
            throw new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_MONTH_IN_PROGRESS.message());
        }
    }
}

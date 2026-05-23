package com.creatorsettlement.application.settlement.dto;

import java.time.LocalDateTime;
import java.time.YearMonth;

public record ConfirmSettlementCommand(
        Long creatorId,
        YearMonth yearMonth,
        LocalDateTime confirmedAt
) {}

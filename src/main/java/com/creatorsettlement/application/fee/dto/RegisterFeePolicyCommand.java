package com.creatorsettlement.application.fee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterFeePolicyCommand(
        BigDecimal rate,
        LocalDate effectiveFrom
) {
}

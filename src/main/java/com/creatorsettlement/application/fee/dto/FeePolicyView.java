package com.creatorsettlement.application.fee.dto;

import com.creatorsettlement.domain.repository.fee.dto.FeePolicyRecord;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeePolicyView(
        Long id,
        BigDecimal rate,
        LocalDate effectiveFrom
) {
    public static FeePolicyView from(FeePolicyRecord record) {
        return new FeePolicyView(
                record.id(),
                record.policy().rate().value(),
                record.policy().effectiveFrom()
        );
    }
}

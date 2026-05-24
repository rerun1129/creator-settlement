package com.creatorsettlement.presentation.fee.dto;

import com.creatorsettlement.application.fee.dto.FeePolicyView;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FeePolicyResponse(
        Long id,
        BigDecimal rate,
        LocalDate effectiveFrom
) {
    public static FeePolicyResponse from(FeePolicyView view) {
        return new FeePolicyResponse(view.id(), view.rate(), view.effectiveFrom());
    }
}

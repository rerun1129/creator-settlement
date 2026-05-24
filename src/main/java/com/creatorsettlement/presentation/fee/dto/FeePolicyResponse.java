package com.creatorsettlement.presentation.fee.dto;

import com.creatorsettlement.application.fee.dto.FeePolicyView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FeePolicyResponse(
        Long id,
        BigDecimal rate,
        LocalDate effectiveFrom
) {
    public static FeePolicyResponse from(FeePolicyView view) {
        return new FeePolicyResponse(view.id(), view.rate(), view.effectiveFrom());
    }

    public static List<FeePolicyResponse> fromAll(List<FeePolicyView> views) {
        return views.stream()
                .map(FeePolicyResponse::from)
                .toList();
    }
}

package com.creatorsettlement.presentation.fee.dto;

import com.creatorsettlement.application.fee.dto.RegisterFeePolicyCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegisterFeePolicyRequest(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal rate,
        @NotNull @Future LocalDate effectiveFrom
) {
    public RegisterFeePolicyCommand toCommand() {
        return new RegisterFeePolicyCommand(rate, effectiveFrom);
    }
}

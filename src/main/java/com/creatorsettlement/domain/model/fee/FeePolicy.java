package com.creatorsettlement.domain.model.fee;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.FeeRate;

import java.time.LocalDate;

public class FeePolicy {

    private final FeeRate rate;
    private final LocalDate effectiveFrom;

    private FeePolicy(FeeRate rate, LocalDate effectiveFrom) {
        this.rate = rate;
        this.effectiveFrom = effectiveFrom;
    }

    public static FeePolicy of(FeeRate rate, LocalDate effectiveFrom) {
        if (effectiveFrom == null) {
            throw new IllegalArgumentException(DomainErrorMessage.FEE_POLICY_EFFECTIVE_FROM_NULL.message());
        }
        return new FeePolicy(rate, effectiveFrom);
    }

    public FeeRate rate() {
        return rate;
    }

    public LocalDate effectiveFrom() {
        return effectiveFrom;
    }
}

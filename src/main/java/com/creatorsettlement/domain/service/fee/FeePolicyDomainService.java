package com.creatorsettlement.domain.service.fee;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.repository.fee.FeePolicyRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeePolicyDomainService {

    private final FeePolicyRepository repository;

    public FeePolicyDomainService(FeePolicyRepository repository) {
        this.repository = repository;
    }

    public void ensureUniqueEffectiveFrom(LocalDate effectiveFrom) {
        if (repository.existsByEffectiveFrom(effectiveFrom)) {
            throw new IllegalArgumentException(DomainErrorMessage.FEE_POLICY_DUPLICATE_EFFECTIVE_FROM.message());
        }
    }

    public FeeRate findEffectiveRate(LocalDate referenceDate) {
        LocalDate firstDayOfMonth = referenceDate.withDayOfMonth(1);
        return repository.findEffectiveAt(firstDayOfMonth.minusDays(1))
                .map(FeePolicy::rate)
                .orElse(FeeRate.defaultRate());
    }

    public Map<YearMonth, FeeRate> findEffectiveRates(Set<YearMonth> months) {
        List<FeePolicy> policies = repository.findAllOrderByEffectiveFromDesc().stream()
                .map(record -> record.policy())
                .toList();

        Map<YearMonth, FeeRate> result = new HashMap<>();
        for (YearMonth ym : months) {
            LocalDate cutoff = ym.atDay(1).minusDays(1);
            FeeRate rate = policies.stream()
                    .filter(p -> !p.effectiveFrom().isAfter(cutoff))
                    .findFirst()
                    .map(FeePolicy::rate)
                    .orElse(FeeRate.defaultRate());
            result.put(ym, rate);
        }
        return result;
    }
}

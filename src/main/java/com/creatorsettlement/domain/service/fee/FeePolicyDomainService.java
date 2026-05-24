package com.creatorsettlement.domain.service.fee;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.repository.fee.FeePolicyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
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
}

package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.repository.fee.FeePolicyRepository;
import com.creatorsettlement.domain.repository.fee.dto.FeePolicyRecord;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryFeePolicyRepository implements FeePolicyRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<Long, FeePolicy> store = new ConcurrentHashMap<>();

    @Override
    public void save(FeePolicy policy) {
        boolean duplicate = store.values().stream()
                .anyMatch(existing -> existing.effectiveFrom().equals(policy.effectiveFrom()));
        if (duplicate) {
            throw new IllegalArgumentException(DomainErrorMessage.FEE_POLICY_DUPLICATE_EFFECTIVE_FROM.message());
        }
        Long id = sequence.incrementAndGet();
        store.put(id, policy);
    }

    @Override
    public Optional<FeePolicy> findEffectiveAt(LocalDate referenceDate) {
        return store.values().stream()
                .filter(policy -> !policy.effectiveFrom().isAfter(referenceDate))
                .max(Comparator.comparing(FeePolicy::effectiveFrom));
    }

    @Override
    public List<FeePolicyRecord> findAllOrderByEffectiveFromDesc() {
        return store.entrySet().stream()
                .map(entry -> new FeePolicyRecord(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing((FeePolicyRecord record) -> record.policy().effectiveFrom()).reversed())
                .toList();
    }
}

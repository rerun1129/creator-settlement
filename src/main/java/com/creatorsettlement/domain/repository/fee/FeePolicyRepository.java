package com.creatorsettlement.domain.repository.fee;

import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.repository.fee.dto.FeePolicyRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeePolicyRepository {

    void save(FeePolicy policy);

    boolean existsByEffectiveFrom(LocalDate effectiveFrom);

    Optional<FeePolicy> findEffectiveAt(LocalDate referenceDate);

    List<FeePolicyRecord> findAllOrderByEffectiveFromDesc();
}

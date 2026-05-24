package com.creatorsettlement.infrastructure.persistence.fee;

import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.repository.fee.dto.FeePolicyRecord;

class FeePolicyMapper {

    private FeePolicyMapper() {}

    static FeePolicyJpaEntity toEntity(FeePolicy policy) {
        return FeePolicyJpaEntity.of(policy.rate().value(), policy.effectiveFrom());
    }

    static FeePolicy toDomain(FeePolicyJpaEntity entity) {
        return FeePolicy.of(FeeRate.of(entity.getRate()), entity.getEffectiveFrom());
    }

    static FeePolicyRecord toRecord(FeePolicyJpaEntity entity) {
        return new FeePolicyRecord(entity.getId(), toDomain(entity));
    }
}

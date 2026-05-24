package com.creatorsettlement.infrastructure.persistence.fee;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.fee.FeePolicy;
import com.creatorsettlement.domain.repository.fee.FeePolicyRepository;
import com.creatorsettlement.domain.repository.fee.dto.FeePolicyRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaFeePolicyRepository implements FeePolicyRepository {

    private final FeePolicyJpaDataRepository dataRepository;

    public JpaFeePolicyRepository(FeePolicyJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void save(FeePolicy policy) {
        try {
            dataRepository.save(FeePolicyMapper.toEntity(policy));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException(DomainErrorMessage.FEE_POLICY_DUPLICATE_EFFECTIVE_FROM.message(), e);
        }
    }

    @Override
    public Optional<FeePolicy> findEffectiveAt(LocalDate referenceDate) {
        return dataRepository.findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(referenceDate)
                .map(FeePolicyMapper::toDomain);
    }

    @Override
    public List<FeePolicyRecord> findAllOrderByEffectiveFromDesc() {
        return dataRepository.findAllByOrderByEffectiveFromDesc().stream()
                .map(FeePolicyMapper::toRecord)
                .toList();
    }
}

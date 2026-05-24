package com.creatorsettlement.infrastructure.persistence.fee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FeePolicyJpaDataRepository extends JpaRepository<FeePolicyJpaEntity, Long> {

    Optional<FeePolicyJpaEntity> findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate referenceDate);

    List<FeePolicyJpaEntity> findAllByOrderByEffectiveFromDesc();
}

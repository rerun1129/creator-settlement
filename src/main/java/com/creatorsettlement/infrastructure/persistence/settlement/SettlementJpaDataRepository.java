package com.creatorsettlement.infrastructure.persistence.settlement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementJpaDataRepository extends JpaRepository<SettlementJpaEntity, Long> {

    Optional<SettlementJpaEntity> findByCreatorIdAndYearMonth(Long creatorId, String yearMonth);
}

package com.creatorsettlement.infrastructure.persistence.sales;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface CancellationJpaDataRepository extends JpaRepository<CancellationRecordJpaEntity, Long> {

    List<CancellationRecordJpaEntity> findBySalesRecordId(Long salesRecordId);

    List<CancellationRecordJpaEntity> findAllBySalesRecordIdIn(Collection<Long> salesRecordIds);
}

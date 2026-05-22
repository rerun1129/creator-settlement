package com.creatorsettlement.infrastructure.persistence.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

interface CancellationJpaDataRepository extends JpaRepository<CancellationRecordJpaEntity, Long> {

    @Query("SELECT COALESCE(SUM(c.refundAmount), 0) FROM CancellationRecordJpaEntity c WHERE c.salesRecordId = :salesRecordId")
    BigDecimal sumRefundAmountBySalesRecordId(@Param("salesRecordId") Long salesRecordId);

    List<CancellationRecordJpaEntity> findAllBySalesRecordIdIn(Collection<Long> salesRecordIds);
}

package com.creatorsettlement.infrastructure.persistence.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface CancellationJpaDataRepository extends JpaRepository<CancellationRecordJpaEntity, Long> {

    List<CancellationRecordJpaEntity> findBySalesRecordId(Long salesRecordId);

    List<CancellationRecordJpaEntity> findAllBySalesRecordIdIn(Collection<Long> salesRecordIds);

    @Query("SELECT c FROM CancellationRecordJpaEntity c WHERE c.cancelledAt >= :from AND c.cancelledAt < :toExclusive")
    List<CancellationRecordJpaEntity> findByCancelledAtBetween(
        @Param("from") LocalDateTime from,
        @Param("toExclusive") LocalDateTime toExclusive
    );

    @Query("""
        SELECT c FROM CancellationRecordJpaEntity c
        WHERE c.salesRecordId IN (
            SELECT s.id FROM SalesRecordJpaEntity s WHERE s.course.creatorId = :creatorId
        )
          AND c.cancelledAt >= :startInclusive AND c.cancelledAt < :endExclusive
    """)
    List<CancellationRecordJpaEntity> findByCreatorAndCancelledAtBetween(
        @Param("creatorId") Long creatorId,
        @Param("startInclusive") LocalDateTime startInclusive,
        @Param("endExclusive") LocalDateTime endExclusive
    );

    @Query("""
        SELECT new com.creatorsettlement.infrastructure.persistence.sales.MonthlyCancellationAggregateRow(
            s.course.creatorId,
            YEAR(cn.cancelledAt),
            MONTH(cn.cancelledAt),
            SUM(cn.refundAmount)
        )
        FROM CancellationRecordJpaEntity cn, SalesRecordJpaEntity s
        WHERE cn.salesRecordId = s.id
          AND cn.cancelledAt >= :from AND cn.cancelledAt < :toExclusive
        GROUP BY s.course.creatorId, YEAR(cn.cancelledAt), MONTH(cn.cancelledAt)
    """)
    List<MonthlyCancellationAggregateRow> aggregateMonthlyCancellations(
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive
    );
}

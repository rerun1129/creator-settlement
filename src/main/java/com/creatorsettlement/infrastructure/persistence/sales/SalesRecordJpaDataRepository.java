package com.creatorsettlement.infrastructure.persistence.sales;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SalesRecordJpaDataRepository extends JpaRepository<SalesRecordJpaEntity, Long> {

    @Query("SELECT s FROM SalesRecordJpaEntity s JOIN FETCH s.course c WHERE c.creatorId = :creatorId AND s.paidAt >= :from AND s.paidAt < :to")
    List<SalesRecordJpaEntity> findByCreatorIdAndPeriod(
            @Param("creatorId") Long creatorId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT s FROM SalesRecordJpaEntity s JOIN FETCH s.course WHERE s.paidAt >= :from AND s.paidAt < :to")
    List<SalesRecordJpaEntity> findByPeriod(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT s FROM SalesRecordJpaEntity s JOIN FETCH s.course WHERE s.id IN :ids")
    List<SalesRecordJpaEntity> findAllByIdIn(@Param("ids") Collection<Long> ids);

    List<SalesRecordJpaEntity> findByCourse_IdAndStudentId(Long courseId, Long studentId);
}

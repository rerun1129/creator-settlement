package com.creatorsettlement.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

interface CourseJpaDataRepository extends JpaRepository<CourseJpaEntity, Long> {

    @Query("SELECT c.id FROM CourseJpaEntity c WHERE c.creatorId = :creatorId")
    List<Long> findIdsByCreatorId(@Param("creatorId") Long creatorId);

    List<CourseCreatorRow> findByIdIn(Collection<Long> ids);

    interface CourseCreatorRow {
        Long getId();
        Long getCreatorId();
    }
}

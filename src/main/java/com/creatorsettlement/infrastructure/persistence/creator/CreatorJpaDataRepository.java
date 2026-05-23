package com.creatorsettlement.infrastructure.persistence.creator;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CreatorJpaDataRepository extends JpaRepository<CreatorJpaEntity, Long> {

    @Query("SELECT c.id FROM CreatorJpaEntity c")
    List<Long> findAllIds();
}

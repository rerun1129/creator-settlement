package com.creatorsettlement.infrastructure.persistence.creator;

import org.springframework.data.jpa.repository.JpaRepository;

interface CreatorJpaDataRepository extends JpaRepository<CreatorJpaEntity, Long> {
}

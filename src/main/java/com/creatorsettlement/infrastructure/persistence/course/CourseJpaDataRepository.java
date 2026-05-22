package com.creatorsettlement.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

interface CourseJpaDataRepository extends JpaRepository<CourseJpaEntity, Long> {
}

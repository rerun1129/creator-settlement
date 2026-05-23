package com.creatorsettlement.infrastructure.persistence.course;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseJpaDataRepository extends JpaRepository<CourseJpaEntity, Long> {
}

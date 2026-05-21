package com.creatorsettlement.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course")
class CourseJpaEntity {

    @Id
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false)
    private String title;

    protected CourseJpaEntity() {
    }

    static CourseJpaEntity of(Long id, Long creatorId, String title) {
        CourseJpaEntity entity = new CourseJpaEntity();
        entity.id = id;
        entity.creatorId = creatorId;
        entity.title = title;
        return entity;
    }

    Long getId() {
        return id;
    }

    Long getCreatorId() {
        return creatorId;
    }

    String getTitle() {
        return title;
    }
}

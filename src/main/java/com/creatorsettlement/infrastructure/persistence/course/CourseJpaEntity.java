package com.creatorsettlement.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course")
public class CourseJpaEntity {

    @Id
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false)
    private String title;

    protected CourseJpaEntity() {
    }

    public static CourseJpaEntity of(Long id, Long creatorId, String title) {
        CourseJpaEntity entity = new CourseJpaEntity();
        entity.id = id;
        entity.creatorId = creatorId;
        entity.title = title;
        return entity;
    }

    public Long getId() {
        return id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public String getTitle() {
        return title;
    }
}

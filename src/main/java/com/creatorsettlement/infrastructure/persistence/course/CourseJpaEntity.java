package com.creatorsettlement.infrastructure.persistence.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "course")
public class CourseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(nullable = false)
    private String title;

    protected CourseJpaEntity() {
    }

    public static CourseJpaEntity of(Long creatorId, String title) {
        CourseJpaEntity entity = new CourseJpaEntity();
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

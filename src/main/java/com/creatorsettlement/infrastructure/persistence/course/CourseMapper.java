package com.creatorsettlement.infrastructure.persistence.course;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;

class CourseMapper {

    private CourseMapper() {
    }

    static CourseJpaEntity toEntity(Course course) {
        return CourseJpaEntity.of(
                course.courseId().value(),
                course.creatorId().value(),
                course.title()
        );
    }

    static Course toDomain(CourseJpaEntity entity) {
        return Course.of(
                CourseId.of(entity.getId()),
                CreatorId.of(entity.getCreatorId()),
                entity.getTitle()
        );
    }
}

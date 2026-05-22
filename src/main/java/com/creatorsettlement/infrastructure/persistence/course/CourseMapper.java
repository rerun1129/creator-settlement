package com.creatorsettlement.infrastructure.persistence.course;

import com.creatorsettlement.domain.model.course.Course;

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
}

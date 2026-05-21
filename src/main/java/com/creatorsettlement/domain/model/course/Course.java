package com.creatorsettlement.domain.model.course;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;

public record Course(CourseId courseId, CreatorId creatorId, String title) {

    public Course {
        if (courseId == null) {
            throw new IllegalArgumentException(DomainErrorMessage.COURSE_ID_NULL.message());
        }
        if (creatorId == null) {
            throw new IllegalArgumentException(DomainErrorMessage.CREATOR_ID_NULL.message());
        }
    }

    public static Course of(CourseId courseId, CreatorId creatorId, String title) {
        return new Course(courseId, creatorId, title);
    }
}

package com.creatorsettlement.domain.model.course;

import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;

public record Course(CourseId courseId, CreatorId creatorId, String title) {

    public static Course of(CourseId courseId, CreatorId creatorId, String title) {
        return new Course(courseId, creatorId, title);
    }
}

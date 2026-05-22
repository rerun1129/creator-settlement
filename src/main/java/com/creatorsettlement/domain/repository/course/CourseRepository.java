package com.creatorsettlement.domain.repository.course;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.vo.CourseId;

public interface CourseRepository {

    void saveCourse(Course course);

    boolean existsByCourseId(CourseId courseId);
}

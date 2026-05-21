package com.creatorsettlement.domain.repository;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;

import java.util.List;

public interface CourseRepository {

    void saveCourse(Course course);

    List<CourseId> findCourseIdsByCreatorId(CreatorId creatorId);
}

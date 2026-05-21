package com.creatorsettlement.domain.repository;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CourseRepository {

    void saveCourse(Course course);

    boolean existsByCourseId(CourseId courseId);

    List<CourseId> findCourseIdsByCreatorId(CreatorId creatorId);

    Map<CourseId, CreatorId> findCreatorIdsByCourseIds(Collection<CourseId> courseIds);
}

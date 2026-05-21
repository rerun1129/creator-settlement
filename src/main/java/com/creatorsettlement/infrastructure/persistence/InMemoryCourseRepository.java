package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.CourseRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryCourseRepository implements CourseRepository {

    private final ConcurrentMap<CourseId, Course> coursesById = new ConcurrentHashMap<>();

    @Override
    public void saveCourse(Course course) {
        coursesById.put(course.courseId(), course);
    }

    @Override
    public boolean existsByCourseId(CourseId courseId) {
        return coursesById.containsKey(courseId);
    }

    @Override
    public List<CourseId> findCourseIdsByCreatorId(CreatorId creatorId) {
        return coursesById.values().stream()
                .filter(course -> course.creatorId().equals(creatorId))
                .map(Course::courseId)
                .toList();
    }

    @Override
    public Map<CourseId, CreatorId> findCreatorIdsByCourseIds(Collection<CourseId> courseIds) {
        Set<CourseId> ids = new HashSet<>(courseIds);
        return coursesById.values().stream()
                .filter(course -> ids.contains(course.courseId()))
                .collect(Collectors.toMap(Course::courseId, Course::creatorId));
    }
}

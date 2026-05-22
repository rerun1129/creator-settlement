package com.creatorsettlement.infrastructure.persistence.course;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.repository.course.CourseRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaCourseRepository implements CourseRepository {

    private final CourseJpaDataRepository dataRepository;

    public JpaCourseRepository(CourseJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void saveCourse(Course course) {
        dataRepository.save(CourseMapper.toEntity(course));
    }

    @Override
    public boolean existsByCourseId(CourseId courseId) {
        return dataRepository.existsById(courseId.value());
    }
}

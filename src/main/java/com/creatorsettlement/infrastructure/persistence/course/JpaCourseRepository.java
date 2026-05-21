package com.creatorsettlement.infrastructure.persistence.course;

import com.creatorsettlement.domain.model.course.Course;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.CourseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Profile("jpa")
@Repository
public class JpaCourseRepository implements CourseRepository {

    private final CourseJpaDataRepository dataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaCourseRepository(CourseJpaDataRepository dataRepository) {
        this.dataRepository = dataRepository;
    }

    @Override
    public void saveCourse(Course course) {
        entityManager.persist(CourseMapper.toEntity(course));
    }

    @Override
    public boolean existsByCourseId(CourseId courseId) {
        return dataRepository.existsById(courseId.value());
    }

    @Override
    public List<CourseId> findCourseIdsByCreatorId(CreatorId creatorId) {
        return dataRepository.findIdsByCreatorId(creatorId.value())
                .stream()
                .map(CourseId::of)
                .toList();
    }

    @Override
    public Map<CourseId, CreatorId> findCreatorIdsByCourseIds(Collection<CourseId> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = courseIds.stream().map(CourseId::value).toList();
        return dataRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(
                        r -> CourseId.of(r.getId()),
                        r -> CreatorId.of(r.getCreatorId())
                ));
    }
}

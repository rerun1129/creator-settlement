package com.creatorsettlement.domain.model.course;

import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Course 엔티티")
class CourseTest {

    @Test
    @DisplayName("정상 입력이면 Course가 생성되고 필드를 반환한다")
    void of_returnsCourse_whenInputIsValid() {
        // Given
        CourseId courseId = CourseId.of(10L);
        CreatorId creatorId = CreatorId.of(100L);
        String title = "도메인 주도 설계";

        // When
        Course course = Course.of(courseId, creatorId, title);

        // Then
        assertThat(course.courseId()).isEqualTo(courseId);
        assertThat(course.creatorId()).isEqualTo(creatorId);
        assertThat(course.title()).isEqualTo(title);
    }

}

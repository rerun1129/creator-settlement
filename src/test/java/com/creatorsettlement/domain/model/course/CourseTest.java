package com.creatorsettlement.domain.model.course;

import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @DisplayName("courseId가 null이면 IllegalArgumentException을 던진다")
    void of_throwsException_whenCourseIdIsNull() {
        // Given
        CourseId courseId = null;
        CreatorId creatorId = CreatorId.of(100L);

        // When / Then
        assertThatThrownBy(() -> Course.of(courseId, creatorId, "title 더미"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("creatorId가 null이면 IllegalArgumentException을 던진다")
    void of_throwsException_whenCreatorIdIsNull() {
        // Given
        CourseId courseId = CourseId.of(10L);
        CreatorId creatorId = null;

        // When / Then
        assertThatThrownBy(() -> Course.of(courseId, creatorId, "title 더미"))
                .isInstanceOf(IllegalArgumentException.class);
    }

}

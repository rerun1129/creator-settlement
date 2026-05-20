package com.creatorsettlement.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CourseId 값 객체 단위 테스트")
class CourseIdTest {

    @Test
    @DisplayName("정상 Long 값으로 생성되고 value()가 같은지 확인한다")
    void should_create_course_id_when_value_is_present() {
        // given
        Long value = 1L;

        // when
        CourseId courseId = new CourseId(value);

        // then
        assertThat(courseId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("null 값으로 생성 시 IllegalArgumentException이 발생한다")
    void should_throw_when_value_is_null() {
        // given & when & then
        assertThatThrownBy(() -> new CourseId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Course ID는 null일 수 없습니다");
    }

    @Test
    @DisplayName("같은 value를 가지면 equals가 true이다")
    void should_be_equal_when_values_are_same() {
        // given
        CourseId courseId1 = new CourseId(42L);
        CourseId courseId2 = new CourseId(42L);

        // when & then
        assertThat(courseId1).isEqualTo(courseId2);
    }

    @Test
    @DisplayName("다른 value를 가지면 equals가 false이다")
    void should_not_be_equal_when_values_differ() {
        // given
        CourseId courseId1 = new CourseId(1L);
        CourseId courseId2 = new CourseId(2L);

        // when & then
        assertThat(courseId1).isNotEqualTo(courseId2);
    }
}

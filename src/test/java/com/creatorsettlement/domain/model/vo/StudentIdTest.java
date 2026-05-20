package com.creatorsettlement.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("StudentId 값 객체 단위 테스트")
class StudentIdTest {

    @Test
    @DisplayName("정상 Long 값으로 생성되고 value()가 같은지 확인한다")
    void should_create_student_id_when_value_is_present() {
        // given
        Long value = 1L;

        // when
        StudentId studentId = new StudentId(value);

        // then
        assertThat(studentId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("null 값으로 생성 시 IllegalArgumentException이 발생한다")
    void should_throw_when_value_is_null() {
        // given & when & then
        assertThatThrownBy(() -> new StudentId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Student ID는 null일 수 없습니다");
    }

    @Test
    @DisplayName("같은 value를 가지면 equals가 true이다")
    void should_be_equal_when_values_are_same() {
        // given
        StudentId studentId1 = new StudentId(42L);
        StudentId studentId2 = new StudentId(42L);

        // when & then
        assertThat(studentId1).isEqualTo(studentId2);
    }

    @Test
    @DisplayName("다른 value를 가지면 equals가 false이다")
    void should_not_be_equal_when_values_differ() {
        // given
        StudentId studentId1 = new StudentId(1L);
        StudentId studentId2 = new StudentId(2L);

        // when & then
        assertThat(studentId1).isNotEqualTo(studentId2);
    }
}

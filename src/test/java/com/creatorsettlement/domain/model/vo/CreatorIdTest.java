package com.creatorsettlement.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CreatorId VO")
class CreatorIdTest {

    @Test
    @DisplayName("정상 입력이면 CreatorId가 생성되고 value를 반환한다")
    void of_returnsCreatorId_whenInputIsValid() {
        // Given
        Long input = 1L;

        // When
        CreatorId creatorId = CreatorId.of(input);

        // Then
        assertThat(creatorId.value()).isEqualTo(input);
    }

    @Test
    @DisplayName("value가 null이면 IllegalArgumentException을 던진다")
    void of_throwsException_whenValueIsNull() {
        // Given
        Long input = null;

        // When / Then
        assertThatThrownBy(() -> CreatorId.of(input))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

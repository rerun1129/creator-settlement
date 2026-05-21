package com.creatorsettlement.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ListSalesQuery 검증")
class ListSalesQueryTest {

    @Test
    @DisplayName("from이 null이면 IllegalArgumentException을 던진다")
    void throwsException_whenFromIsNull() {
        // Given
        Long creatorId = 100L;
        LocalDateTime from = null;
        LocalDateTime toExclusive = LocalDateTime.of(2026, 5, 1, 0, 0);

        // When / Then
        assertThatThrownBy(() -> new ListSalesQuery(creatorId, from, toExclusive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toExclusive가 null이면 IllegalArgumentException을 던진다")
    void throwsException_whenToExclusiveIsNull() {
        // Given
        Long creatorId = 100L;
        LocalDateTime from = LocalDateTime.of(2026, 4, 1, 0, 0);
        LocalDateTime toExclusive = null;

        // When / Then
        assertThatThrownBy(() -> new ListSalesQuery(creatorId, from, toExclusive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("from이 toExclusive보다 이후이면 IllegalArgumentException을 던진다")
    void throwsException_whenFromIsAfterToExclusive() {
        // Given
        Long creatorId = 100L;
        LocalDateTime from = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime toExclusive = LocalDateTime.of(2026, 4, 1, 0, 0);

        // When / Then
        assertThatThrownBy(() -> new ListSalesQuery(creatorId, from, toExclusive))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

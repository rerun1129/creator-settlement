package com.creatorsettlement.application;

import com.creatorsettlement.domain.model.vo.CreatorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ListSalesQuery 검증")
class ListSalesQueryTest {

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

    @Test
    @DisplayName("creatorId가 주어지면 Optional<CreatorId>로 감싸 반환한다")
    void toCreatorId_returnsOptionalOfCreatorId_whenCreatorIdIsPresent() {
        // Given
        Long creatorId = 100L;
        ListSalesQuery query = new ListSalesQuery(
                creatorId,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 5, 1, 0, 0)
        );

        // When
        Optional<CreatorId> result = query.toCreatorId();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(CreatorId.of(creatorId));
    }

    @Test
    @DisplayName("creatorId가 null이면 Optional.empty()를 반환한다")
    void toCreatorId_returnsEmpty_whenCreatorIdIsNull() {
        // Given
        ListSalesQuery query = new ListSalesQuery(
                null,
                LocalDateTime.of(2026, 4, 1, 0, 0),
                LocalDateTime.of(2026, 5, 1, 0, 0)
        );

        // When
        Optional<CreatorId> result = query.toCreatorId();

        // Then
        assertThat(result).isEmpty();
    }
}

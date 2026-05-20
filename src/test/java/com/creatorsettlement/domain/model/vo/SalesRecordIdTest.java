package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SalesRecordId 값 객체 단위 테스트")
class SalesRecordIdTest {

    @Test
    @DisplayName("정상 Long 값으로 생성되고 value()가 같은지 확인한다")
    void should_create_sales_record_id_when_value_is_present() {
        // given
        Long value = 1L;

        // when
        SalesRecordId salesRecordId = SalesRecordId.of(value);

        // then
        assertThat(salesRecordId.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("null 값으로 생성 시 IllegalArgumentException이 발생한다")
    void should_throw_when_value_is_null() {
        // given & when & then
        assertThatThrownBy(() -> SalesRecordId.of(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(DomainErrorMessage.SALES_RECORD_ID_NULL.message());
    }

    @Test
    @DisplayName("같은 value를 가지면 equals가 true이다")
    void should_be_equal_when_values_are_same() {
        // given
        SalesRecordId salesRecordId1 = SalesRecordId.of(42L);
        SalesRecordId salesRecordId2 = SalesRecordId.of(42L);

        // when & then
        assertThat(salesRecordId1).isEqualTo(salesRecordId2);
    }

    @Test
    @DisplayName("다른 value를 가지면 equals가 false이다")
    void should_not_be_equal_when_values_differ() {
        // given
        SalesRecordId salesRecordId1 = SalesRecordId.of(1L);
        SalesRecordId salesRecordId2 = SalesRecordId.of(2L);

        // when & then
        assertThat(salesRecordId1).isNotEqualTo(salesRecordId2);
    }
}

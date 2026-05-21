package com.creatorsettlement.domain.model.sale;

import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CancellationRecord 도메인 단위 테스트")
class CancellationRecordTest {

    @Test
    @DisplayName("정상 입력이면 취소 내역이 생성된다")
    void should_create_cancellation_record_when_input_is_valid() {
        // given
        SalesRecordId salesRecordId = SalesRecordId.of(1L);
        Money refundAmount = Money.of(new BigDecimal("10000"));
        OccurredAt cancelledAt = OccurredAt.of(LocalDateTime.now().minusHours(1));

        // when
        CancellationRecord record = CancellationRecord.of(salesRecordId, refundAmount, cancelledAt);

        // then
        assertThat(record.getSalesRecordId()).isEqualTo(salesRecordId);
        assertThat(record.getRefundAmount()).isEqualTo(refundAmount);
        assertThat(record.getCancelledAt()).isEqualTo(cancelledAt);
    }
}

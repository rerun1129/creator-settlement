package com.creatorsettlement.domain.model.settlement;

import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SettlementAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Settlement 단위 테스트")
class SettlementTest {

    private static final CreatorId CREATOR_ID = CreatorId.of(1L);
    private static final YearMonth YEAR_MONTH = YearMonth.of(2026, 5);
    private static final Money TOTAL_SALES = Money.of(new BigDecimal("100000"));
    private static final Money TOTAL_REFUND = Money.of(new BigDecimal("30000"));
    private static final SettlementAmount NET_SALES = SettlementAmount.of(new BigDecimal("70000"));
    private static final FeeRate FEE_RATE = FeeRate.of(new BigDecimal("0.2"));
    private static final Money PLATFORM_FEE = Money.of(new BigDecimal("14000"));
    private static final SettlementAmount EXPECTED_PAYOUT = SettlementAmount.of(new BigDecimal("56000"));

    @Test
    @DisplayName("pendingSnapshot은 PENDING 상태와 null confirmedAt으로 생성한다")
    void pendingSnapshot_creates_pending_status_with_null_confirmedAt() {
        // when
        Settlement settlement = Settlement.pendingSnapshot(
                CREATOR_ID, YEAR_MONTH,
                TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                5L, 2L
        );

        // then
        assertThat(settlement.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.confirmedAt()).isNull();
    }

    @Test
    @DisplayName("confirmedSnapshot 팩토리는 status=CONFIRMED와 confirmedAt을 갖는 Settlement를 생성한다")
    void confirmedSnapshot_creates_settlement_with_confirmed_status_and_confirmedAt() {
        // given
        OccurredAt confirmedAt = OccurredAt.of(LocalDateTime.of(2026, 5, 10, 14, 0));

        // when
        Settlement settlement = Settlement.confirmedSnapshot(
                CREATOR_ID, YEAR_MONTH,
                TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                10L, 2L, confirmedAt
        );

        // then
        assertThat(settlement.status()).isEqualTo(SettlementStatus.CONFIRMED);
        assertThat(settlement.confirmedAt()).isEqualTo(confirmedAt);
    }
}

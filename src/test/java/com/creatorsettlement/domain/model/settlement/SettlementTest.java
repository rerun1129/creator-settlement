package com.creatorsettlement.domain.model.settlement;

import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SettlementAmount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Settlement 단위 테스트")
class SettlementTest {

    @Nested
    @DisplayName("스냅샷 생성")
    class SnapshotCreation {

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

        @Test
        @DisplayName("paidSnapshot 팩토리는 status=PAID와 confirmedAt·paidAt 모두를 가지는 Settlement를 생성한다")
        void paidSnapshot_creates_settlement_with_paid_status_and_both_timestamps() {
            // given
            OccurredAt confirmedAt = OccurredAt.of(LocalDateTime.of(2026, 5, 10, 14, 0));
            OccurredAt paidAt = OccurredAt.of(LocalDateTime.of(2026, 6, 1, 9, 0));

            // when
            Settlement settlement = Settlement.paidSnapshot(
                    CREATOR_ID, YEAR_MONTH,
                    TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                    FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                    10L, 2L, confirmedAt, paidAt
            );

            // then
            assertThat(settlement.status()).isEqualTo(SettlementStatus.PAID);
            assertThat(settlement.confirmedAt()).isEqualTo(confirmedAt);
            assertThat(settlement.paidAt()).isEqualTo(paidAt);
        }

        @Test
        @DisplayName("paidSnapshot에 null confirmedAt 전달 시 발생 일시가 누락되었다는 예외가 발생한다")
        void paidSnapshot_with_null_confirmedAt_throws_OCCURRED_AT_NULL() {
            // when & then
            assertThatThrownBy(() -> OccurredAt.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("발생 일시가 누락되었습니다");
        }

        @Test
        @DisplayName("paidSnapshot에 null paidAt 전달 시 발생 일시가 누락되었다는 예외가 발생한다")
        void paidSnapshot_with_null_paidAt_throws_OCCURRED_AT_NULL() {
            // when & then
            assertThatThrownBy(() -> OccurredAt.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("발생 일시가 누락되었습니다");
        }
    }

    @Nested
    @DisplayName("상태 전이 성공")
    class StatusTransitionSuccess {

        private static final CreatorId CREATOR_ID = CreatorId.of(1L);
        private static final YearMonth YEAR_MONTH = YearMonth.of(2026, 5);
        private static final Money TOTAL_SALES = Money.of(new BigDecimal("100000"));
        private static final Money TOTAL_REFUND = Money.of(new BigDecimal("30000"));
        private static final SettlementAmount NET_SALES = SettlementAmount.of(new BigDecimal("70000"));
        private static final FeeRate FEE_RATE = FeeRate.of(new BigDecimal("0.2"));
        private static final Money PLATFORM_FEE = Money.of(new BigDecimal("14000"));
        private static final SettlementAmount EXPECTED_PAYOUT = SettlementAmount.of(new BigDecimal("56000"));

        @Test
        @DisplayName("PENDING Settlement에 confirm을 호출하면 status가 CONFIRMED로 전이되고 confirmedAt이 설정된다")
        void pendingSettlement_confirms_to_CONFIRMED_with_confirmedAt() {
            // given
            Settlement settlement = pending();
            OccurredAt confirmedAt = at(2026, 5, 10, 14, 0);

            // when
            settlement.confirm(confirmedAt);

            // then
            assertThat(settlement.status()).isEqualTo(SettlementStatus.CONFIRMED);
            assertThat(settlement.confirmedAt()).isEqualTo(confirmedAt);
        }

        @Test
        @DisplayName("CONFIRMED Settlement에 pay 호출 시 status가 PAID로 전이되고 paidAt이 설정된다")
        void confirmedSettlement_pays_to_PAID_with_paidAt() {
            // given
            Settlement settlement = confirmed();
            OccurredAt paidAt = at(2026, 6, 1, 9, 0);

            // when
            settlement.pay(paidAt);

            // then
            assertThat(settlement.status()).isEqualTo(SettlementStatus.PAID);
            assertThat(settlement.confirmedAt()).isEqualTo(at(2026, 5, 10, 14, 0));
            assertThat(settlement.paidAt()).isEqualTo(paidAt);
        }

        private Settlement pending() {
            return Settlement.pendingSnapshot(
                    CREATOR_ID, YEAR_MONTH,
                    TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                    FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                    5L, 2L
            );
        }

        private Settlement confirmed() {
            return Settlement.confirmedSnapshot(
                    CREATOR_ID, YEAR_MONTH,
                    TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                    FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                    10L, 2L, at(2026, 5, 10, 14, 0)
            );
        }

        private OccurredAt at(int year, int month, int day, int hour, int minute) {
            return OccurredAt.of(LocalDateTime.of(year, month, day, hour, minute));
        }
    }

    @Nested
    @DisplayName("상태 전이 예외")
    class StatusTransitionException {

        private static final CreatorId CREATOR_ID = CreatorId.of(1L);
        private static final YearMonth YEAR_MONTH = YearMonth.of(2026, 5);
        private static final Money TOTAL_SALES = Money.of(new BigDecimal("100000"));
        private static final Money TOTAL_REFUND = Money.of(new BigDecimal("30000"));
        private static final SettlementAmount NET_SALES = SettlementAmount.of(new BigDecimal("70000"));
        private static final FeeRate FEE_RATE = FeeRate.of(new BigDecimal("0.2"));
        private static final Money PLATFORM_FEE = Money.of(new BigDecimal("14000"));
        private static final SettlementAmount EXPECTED_PAYOUT = SettlementAmount.of(new BigDecimal("56000"));

        @Test
        @DisplayName("PENDING Settlement에 pay 호출 시 확정되지 않은 정산은 지급할 수 없다는 예외가 발생한다")
        void pendingSettlement_pay_throws_NOT_CONFIRMED_FOR_PAYMENT() {
            // given
            Settlement settlement = pending();

            // when & then
            assertThatThrownBy(() -> settlement.pay(at(2026, 6, 1, 9, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("확정되지 않은 정산은 지급할 수 없습니다");
        }

        @Test
        @DisplayName("CONFIRMED Settlement에 confirm 재호출 시 이미 확정된 정산이라는 예외가 발생한다")
        void confirmedSettlement_confirm_throws_ALREADY_CONFIRMED() {
            // given
            Settlement settlement = confirmed();

            // when & then
            assertThatThrownBy(() -> settlement.confirm(at(2026, 5, 11, 10, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 확정된 정산입니다");
        }

        @Test
        @DisplayName("PAID Settlement에 confirm 호출 시 이미 지급된 정산이라는 예외가 발생한다")
        void paidSettlement_confirm_throws_ALREADY_PAID() {
            // given
            Settlement settlement = paid();

            // when & then
            assertThatThrownBy(() -> settlement.confirm(at(2026, 6, 2, 10, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지급된 정산입니다");
        }

        @Test
        @DisplayName("PAID Settlement에 pay 재호출 시 이미 지급된 정산이라는 예외가 발생한다")
        void paidSettlement_pay_throws_ALREADY_PAID() {
            // given
            Settlement settlement = paid();

            // when & then
            assertThatThrownBy(() -> settlement.pay(at(2026, 6, 2, 10, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지급된 정산입니다");
        }

        private Settlement pending() {
            return Settlement.pendingSnapshot(
                    CREATOR_ID, YEAR_MONTH,
                    TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                    FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                    5L, 2L
            );
        }

        private Settlement confirmed() {
            return Settlement.confirmedSnapshot(
                    CREATOR_ID, YEAR_MONTH,
                    TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                    FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                    10L, 2L, at(2026, 5, 10, 14, 0)
            );
        }

        private Settlement paid() {
            return Settlement.paidSnapshot(
                    CREATOR_ID, YEAR_MONTH,
                    TOTAL_SALES, TOTAL_REFUND, NET_SALES,
                    FEE_RATE, PLATFORM_FEE, EXPECTED_PAYOUT,
                    10L, 2L, at(2026, 5, 10, 14, 0), at(2026, 6, 1, 9, 0)
            );
        }

        private OccurredAt at(int year, int month, int day, int hour, int minute) {
            return OccurredAt.of(LocalDateTime.of(year, month, day, hour, minute));
        }
    }
}

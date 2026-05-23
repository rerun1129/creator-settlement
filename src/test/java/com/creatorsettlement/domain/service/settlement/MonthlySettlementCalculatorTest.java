package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MonthlySettlementCalculator 단위 테스트")
class MonthlySettlementCalculatorTest {

    private MonthlySettlementCalculator calculator;

    private static final CreatorId CREATOR_ID = CreatorId.of(1L);
    private static final YearMonth YEAR_MONTH = YearMonth.of(2026, 5);

    @BeforeEach
    void setUp() {
        calculator = new MonthlySettlementCalculator();
    }

    @Test
    @DisplayName("활동이 없으면 모든 금액과 건수가 0이다")
    void calculates_zero_when_no_activity() {
        // given
        Money totalSales = Money.of(BigDecimal.ZERO);
        Money totalRefund = Money.of(BigDecimal.ZERO);
        FeeRate feeRate = FeeRate.of(new BigDecimal("0.2"));

        // when
        Settlement result = calculator.calculate(CREATOR_ID, YEAR_MONTH, totalSales, totalRefund, 0L, 0L, feeRate);

        // then
        assertThat(result.netSales().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.platformFee().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.expectedPayout().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.salesCount()).isZero();
        assertThat(result.cancellationCount()).isZero();
    }

    @Test
    @DisplayName("환불이 없으면 순매출의 수수료율만큼 수수료를 산정한다")
    void calculates_full_fee_when_no_refund() {
        // given
        Money totalSales = Money.of(new BigDecimal("100000"));
        Money totalRefund = Money.of(BigDecimal.ZERO);
        FeeRate feeRate = FeeRate.of(new BigDecimal("0.2"));

        // when
        Settlement result = calculator.calculate(CREATOR_ID, YEAR_MONTH, totalSales, totalRefund, 10L, 0L, feeRate);

        // then
        assertThat(result.netSales().value()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(result.platformFee().value()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(result.expectedPayout().value()).isEqualByComparingTo(new BigDecimal("80000"));
    }

    @Test
    @DisplayName("부분 환불이 있으면 순매출 기준으로 수수료를 산정한다")
    void calculates_with_partial_refund() {
        // given
        Money totalSales = Money.of(new BigDecimal("100000"));
        Money totalRefund = Money.of(new BigDecimal("30000"));
        FeeRate feeRate = FeeRate.of(new BigDecimal("0.2"));

        // when
        Settlement result = calculator.calculate(CREATOR_ID, YEAR_MONTH, totalSales, totalRefund, 10L, 3L, feeRate);

        // then
        assertThat(result.netSales().value()).isEqualByComparingTo(new BigDecimal("70000"));
        assertThat(result.platformFee().value()).isEqualByComparingTo(new BigDecimal("14000"));
        assertThat(result.expectedPayout().value()).isEqualByComparingTo(new BigDecimal("56000"));
    }

    @Test
    @DisplayName("환불이 판매보다 크면 순매출은 음수가 되고 수수료는 0으로 산정한다")
    void calculates_negative_netSales_when_refund_exceeds_sales() {
        // given
        Money totalSales = Money.of(new BigDecimal("10000"));
        Money totalRefund = Money.of(new BigDecimal("50000"));
        FeeRate feeRate = FeeRate.of(new BigDecimal("0.2"));

        // when
        Settlement result = calculator.calculate(CREATOR_ID, YEAR_MONTH, totalSales, totalRefund, 1L, 5L, feeRate);

        // then
        assertThat(result.netSales().value()).isEqualByComparingTo(new BigDecimal("-40000"));
        assertThat(result.platformFee().value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.expectedPayout().value()).isEqualByComparingTo(new BigDecimal("-40000"));
    }

    @Test
    @DisplayName("플랫폼 수수료는 원 단위로 HALF_UP 반올림한다")
    void rounds_platform_fee_half_up_scale_zero() {
        // given
        // net = 12345, fee = 12345 * 0.2 = 2469.0 → 2469 (HALF_UP, scale 0)
        Money totalSales = Money.of(new BigDecimal("15000"));
        Money totalRefund = Money.of(new BigDecimal("2655"));
        FeeRate feeRate = FeeRate.of(new BigDecimal("0.2"));

        // when
        Settlement result = calculator.calculate(CREATOR_ID, YEAR_MONTH, totalSales, totalRefund, 5L, 1L, feeRate);

        // then
        assertThat(result.platformFee().value()).isEqualByComparingTo(new BigDecimal("2469"));
    }

    @Test
    @DisplayName("입력 건수를 결과에 그대로 보존한다")
    void preserves_counts_in_result() {
        // given
        Money totalSales = Money.of(new BigDecimal("50000"));
        Money totalRefund = Money.of(new BigDecimal("10000"));
        FeeRate feeRate = FeeRate.of(new BigDecimal("0.2"));
        long salesCount = 7L;
        long cancellationCount = 3L;

        // when
        Settlement result = calculator.calculate(CREATOR_ID, YEAR_MONTH, totalSales, totalRefund, salesCount, cancellationCount, feeRate);

        // then
        assertThat(result.salesCount()).isEqualTo(salesCount);
        assertThat(result.cancellationCount()).isEqualTo(cancellationCount);
    }
}

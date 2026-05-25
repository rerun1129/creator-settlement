package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.vo.FeeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("크리에이터 범위 예상 정산금 계산 도메인 서비스 단위 테스트")
class CreatorRangePayoutCalculatorTest {

    private static final FeeRate DEFAULT_RATE = FeeRate.of(new BigDecimal("0.2"));
    private CreatorRangePayoutCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new CreatorRangePayoutCalculator(new SettlementAmountCalculator());
    }

    @Test
    @DisplayName("판매·환불 맵이 모두 비어있으면 0원을 반환한다")
    void calculate_returns_zero_when_both_maps_empty() {
        // When
        BigDecimal result = calculator.calculate(Map.of(), Map.of(), Map.of());

        // Then
        assertThat(result).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("단일 월 sales 50000 + refund 10000 + 수수료율 0.2이면 net 40000의 80% = 32000 반환")
    void calculate_returns_expected_payout_for_single_month() {
        // Given
        YearMonth yearMonth = YearMonth.of(2026, 4);
        Map<YearMonth, BigDecimal> sales = Map.of(yearMonth, new BigDecimal("50000"));
        Map<YearMonth, BigDecimal> refunds = Map.of(yearMonth, new BigDecimal("10000"));
        Map<YearMonth, FeeRate> ratesByMonth = Map.of(yearMonth, DEFAULT_RATE);

        // When
        BigDecimal result = calculator.calculate(sales, refunds, ratesByMonth);

        // Then
        assertThat(result).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("32000"));
    }

    @Test
    @DisplayName("여러 월 sales·refund를 월별로 산정 후 합산한다")
    void calculate_sums_expected_payouts_across_multiple_months() {
        // Given
        YearMonth april = YearMonth.of(2026, 4);
        YearMonth may = YearMonth.of(2026, 5);
        Map<YearMonth, BigDecimal> sales = Map.of(
                april, new BigDecimal("100000"),
                may, new BigDecimal("50000")
        );
        Map<YearMonth, BigDecimal> refunds = Map.of(
                april, new BigDecimal("20000")
        );
        Map<YearMonth, FeeRate> ratesByMonth = Map.of(april, DEFAULT_RATE, may, DEFAULT_RATE);

        // When
        BigDecimal result = calculator.calculate(sales, refunds, ratesByMonth);

        // Then
        assertThat(result).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("104000"));
    }
}

package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SettlementAmountCalculator 도메인 단위 테스트")
class SettlementAmountCalculatorTest {

    private final SettlementAmountCalculator calculator = new SettlementAmountCalculator();

    @Test
    @DisplayName("환불이 없으면 net=totalSales, fee=net*feeRate, expected=net-fee")
    void calculates_expected_payout_when_no_refund() {
        // given
        Money totalSales = Money.of(new BigDecimal("100000"));
        Money totalRefund = Money.of(BigDecimal.ZERO);
        FeeRate feeRate = FeeRate.defaultRate();

        // when
        BigDecimal actual = calculator.calculateExpectedPayout(totalSales, totalRefund, feeRate);

        // then
        BigDecimal expectedNet = new BigDecimal("100000");
        BigDecimal expectedFee = expectedNet.multiply(feeRate.value());
        BigDecimal expected = expectedNet.subtract(expectedFee);
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(expected);
    }

    @Test
    @DisplayName("부분 환불이면 net=sales-refund, fee=net*feeRate, expected=net-fee")
    void calculates_expected_payout_with_partial_refund() {
        // given
        Money totalSales = Money.of(new BigDecimal("100000"));
        Money totalRefund = Money.of(new BigDecimal("20000"));
        FeeRate feeRate = FeeRate.defaultRate();

        // when
        BigDecimal actual = calculator.calculateExpectedPayout(totalSales, totalRefund, feeRate);

        // then
        BigDecimal expectedNet = new BigDecimal("80000");
        BigDecimal expectedFee = expectedNet.multiply(feeRate.value());
        BigDecimal expected = expectedNet.subtract(expectedFee);
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(expected);
    }

    @Test
    @DisplayName("환불이 매출을 초과해 net<0이면 fee=0이고 expected=net(음수)")
    void returns_negative_expected_payout_with_zero_fee_when_refund_exceeds_sales() {
        // given
        Money totalSales = Money.of(new BigDecimal("30000"));
        Money totalRefund = Money.of(new BigDecimal("50000"));
        FeeRate feeRate = FeeRate.defaultRate();

        // when
        BigDecimal actual = calculator.calculateExpectedPayout(totalSales, totalRefund, feeRate);

        // then
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("-20000"));
    }

    @Test
    @DisplayName("매출/환불 모두 0이면 net=0, fee=0, expected=0")
    void returns_zero_expected_payout_when_no_activity() {
        // given
        Money totalSales = Money.of(BigDecimal.ZERO);
        Money totalRefund = Money.of(BigDecimal.ZERO);
        FeeRate feeRate = FeeRate.defaultRate();

        // when
        BigDecimal actual = calculator.calculateExpectedPayout(totalSales, totalRefund, feeRate);

        // then
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("fee는 HALF_UP scale 0으로 반올림된다 (net=12345, rate=0.2 -> fee=2469)")
    void rounds_fee_with_half_up_scale_zero() {
        // given
        Money totalSales = Money.of(new BigDecimal("12345"));
        Money totalRefund = Money.of(BigDecimal.ZERO);
        FeeRate feeRate = FeeRate.defaultRate();

        // when
        BigDecimal actual = calculator.calculateExpectedPayout(totalSales, totalRefund, feeRate);

        // then
        BigDecimal expectedFee = new BigDecimal("2469");
        BigDecimal expected = new BigDecimal("12345").subtract(expectedFee);
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(expected);
    }

    @Test
    @DisplayName(".5 분수 결과는 HALF_UP으로 +1 올림된다 (net=5, rate=0.5 -> fee=3, expected=2)")
    void rounds_fee_with_half_up_when_fraction_is_exactly_half() {
        // given
        Money totalSales = Money.of(new BigDecimal("5"));
        Money totalRefund = Money.of(BigDecimal.ZERO);
        FeeRate feeRate = FeeRate.of(new BigDecimal("0.5"));

        // when
        BigDecimal actual = calculator.calculateExpectedPayout(totalSales, totalRefund, feeRate);

        // then
        assertThat(actual).usingComparator(BigDecimal::compareTo).isEqualTo(new BigDecimal("2"));
    }
}

package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class CreatorRangePayoutCalculator {

    private final SettlementAmountCalculator settlementAmountCalculator;

    public CreatorRangePayoutCalculator(SettlementAmountCalculator settlementAmountCalculator) {
        this.settlementAmountCalculator = settlementAmountCalculator;
    }

    public BigDecimal calculate(
            Map<YearMonth, BigDecimal> salesByMonth,
            Map<YearMonth, BigDecimal> refundsByMonth,
            Map<YearMonth, FeeRate> ratesByMonth
    ) {
        Set<YearMonth> activeMonths = new HashSet<>();
        activeMonths.addAll(salesByMonth.keySet());
        activeMonths.addAll(refundsByMonth.keySet());

        BigDecimal creatorExpected = BigDecimal.ZERO;
        for (YearMonth ym : activeMonths) {
            FeeRate monthRate = ratesByMonth.get(ym);
            BigDecimal monthSales = salesByMonth.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal monthRefund = refundsByMonth.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal monthExpected = settlementAmountCalculator.calculateExpectedPayout(
                    Money.of(monthSales), Money.of(monthRefund), monthRate);
            creatorExpected = creatorExpected.add(monthExpected);
        }
        return creatorExpected;
    }
}

package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.application.fee.FeePolicyService;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public class CreatorRangePayoutCalculator {

    private final SettlementAmountCalculator settlementAmountCalculator;
    private final FeePolicyService feePolicyService;

    public CreatorRangePayoutCalculator(
            SettlementAmountCalculator settlementAmountCalculator,
            FeePolicyService feePolicyService
    ) {
        this.settlementAmountCalculator = settlementAmountCalculator;
        this.feePolicyService = feePolicyService;
    }

    public BigDecimal calculate(
            Map<YearMonth, BigDecimal> salesByMonth,
            Map<YearMonth, BigDecimal> refundsByMonth
    ) {
        Set<YearMonth> activeMonths = new HashSet<>();
        activeMonths.addAll(salesByMonth.keySet());
        activeMonths.addAll(refundsByMonth.keySet());

        Map<YearMonth, FeeRate> rateCache = new HashMap<>();
        BigDecimal creatorExpected = BigDecimal.ZERO;
        for (YearMonth ym : activeMonths) {
            FeeRate monthRate = rateCache.computeIfAbsent(ym, k -> feePolicyService.findEffectiveRate(k.atDay(1)));
            BigDecimal monthSales = salesByMonth.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal monthRefund = refundsByMonth.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal monthExpected = settlementAmountCalculator.calculateExpectedPayout(
                    Money.of(monthSales), Money.of(monthRefund), monthRate);
            creatorExpected = creatorExpected.add(monthExpected);
        }
        return creatorExpected;
    }
}

package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SettlementAmountCalculator {

    public BigDecimal calculateExpectedPayout(Money totalSales, Money totalRefund, FeeRate feeRate) {
        BigDecimal net = totalSales.value().subtract(totalRefund.value());
        BigDecimal fee = (net.signum() < 0)
                ? BigDecimal.ZERO
                : net.multiply(feeRate.value()).setScale(0, RoundingMode.HALF_UP);
        return net.subtract(fee);
    }
}

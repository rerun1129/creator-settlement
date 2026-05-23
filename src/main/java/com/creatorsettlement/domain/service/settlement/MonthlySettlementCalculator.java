package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SettlementAmount;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

public class MonthlySettlementCalculator {

    public Settlement calculate(
            CreatorId creatorId, YearMonth yearMonth,
            Money totalSales, Money totalRefund,
            long salesCount, long cancellationCount,
            FeeRate feeRate
    ) {
        BigDecimal netSalesValue = totalSales.value().subtract(totalRefund.value());
        BigDecimal platformFeeValue;
        if (netSalesValue.signum() < 0) {
            platformFeeValue = BigDecimal.ZERO;
        } else {
            platformFeeValue = netSalesValue.multiply(feeRate.value()).setScale(0, RoundingMode.HALF_UP);
        }
        BigDecimal expectedPayoutValue = netSalesValue.subtract(platformFeeValue);

        return Settlement.pendingSnapshot(
                creatorId, yearMonth,
                totalSales, totalRefund, SettlementAmount.of(netSalesValue),
                feeRate, Money.of(platformFeeValue), SettlementAmount.of(expectedPayoutValue),
                salesCount, cancellationCount
        );
    }
}

package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.application.fee.FeePolicyService;
import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Component
public class PendingSettlementResolver {

    private final SalesRepository salesRepository;
    private final FeePolicyService feePolicyService;
    private final MonthlySettlementCalculator monthlySettlementCalculator;

    public PendingSettlementResolver(
            SalesRepository salesRepository,
            FeePolicyService feePolicyService,
            MonthlySettlementCalculator monthlySettlementCalculator
    ) {
        this.salesRepository = salesRepository;
        this.feePolicyService = feePolicyService;
        this.monthlySettlementCalculator = monthlySettlementCalculator;
    }

    public Settlement resolve(CreatorId creatorId, YearMonth yearMonth) {
        SalesSummary sa = salesRepository.findSalesSummaryByCreatorAndMonth(creatorId, yearMonth);
        CancellationSummary ca = salesRepository.findCancellationSummaryByCreatorAndMonth(creatorId, yearMonth);
        FeeRate feeRate = feePolicyService.findEffectiveRate(yearMonth.atDay(1));
        return monthlySettlementCalculator.calculate(
                creatorId, yearMonth,
                sa.totalAmount(), ca.totalRefund(),
                sa.count(), ca.count(),
                feeRate
        );
    }
}

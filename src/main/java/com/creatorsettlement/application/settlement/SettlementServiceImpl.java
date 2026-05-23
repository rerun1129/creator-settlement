package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final SalesRepository salesRepository;
    private final MonthlySettlementCalculator monthlySettlementCalculator;

    public SettlementServiceImpl(
            SettlementRepository settlementRepository,
            SalesRepository salesRepository,
            MonthlySettlementCalculator monthlySettlementCalculator
    ) {
        this.settlementRepository = settlementRepository;
        this.salesRepository = salesRepository;
        this.monthlySettlementCalculator = monthlySettlementCalculator;
    }

    @Override
    public MonthlySettlementView getMonthlySettlement(MonthlySettlementQuery query) {
        CreatorId creatorId = CreatorId.of(query.creatorId());
        YearMonth ym = query.yearMonth();

        Optional<Settlement> stored = settlementRepository.findByCreatorIdAndYearMonth(creatorId, ym);
        if (stored.isPresent()) {
            return MonthlySettlementView.from(stored.get());
        }

        SalesSummary sa = salesRepository.findSalesSummaryByCreatorAndMonth(creatorId, ym);
        CancellationSummary ca = salesRepository.findCancellationSummaryByCreatorAndMonth(creatorId, ym);
        Settlement pending = monthlySettlementCalculator.calculate(
                creatorId, ym,
                sa.totalAmount(), ca.totalRefund(),
                sa.count(), ca.count(),
                FeeRate.defaultRate()
        );
        return MonthlySettlementView.from(pending);
    }
}

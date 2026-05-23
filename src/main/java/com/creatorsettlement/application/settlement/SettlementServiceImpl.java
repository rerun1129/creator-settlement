package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.settlement.dto.ConfirmSettlementCommand;
import com.creatorsettlement.application.settlement.dto.CreatorPayableView;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementQuery;
import com.creatorsettlement.application.settlement.dto.MonthlySettlementView;
import com.creatorsettlement.application.settlement.dto.PaySettlementCommand;
import com.creatorsettlement.application.settlement.dto.SettlementRangeQuery;
import com.creatorsettlement.application.settlement.dto.SettlementRangeView;
import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.settlement.Settlement;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.FeeRate;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.repository.creator.CreatorRepository;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.CancellationView;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final SalesRepository salesRepository;
    private final CreatorRepository creatorRepository;
    private final MonthlySettlementCalculator monthlySettlementCalculator;

    public SettlementServiceImpl(
            SettlementRepository settlementRepository,
            SalesRepository salesRepository,
            CreatorRepository creatorRepository,
            MonthlySettlementCalculator monthlySettlementCalculator
    ) {
        this.settlementRepository = settlementRepository;
        this.salesRepository = salesRepository;
        this.creatorRepository = creatorRepository;
        this.monthlySettlementCalculator = monthlySettlementCalculator;
    }

    @Override
    public MonthlySettlementView getMonthlySettlement(MonthlySettlementQuery query) {
        CreatorId creatorId = CreatorId.of(query.creatorId());
        YearMonth ym = query.yearMonth();
        Settlement settlement = settlementRepository
                .findByCreatorIdAndYearMonth(creatorId, ym)
                .orElseGet(() -> calculatePending(creatorId, ym));
        return MonthlySettlementView.from(settlement);
    }

    @Override
    @Transactional
    public void confirm(ConfirmSettlementCommand command) {
        CreatorId creatorId = CreatorId.of(command.creatorId());
        YearMonth ym = command.yearMonth();
        Settlement settlement = settlementRepository
                .findByCreatorIdAndYearMonth(creatorId, ym)
                .orElseGet(() -> calculatePending(creatorId, ym));
        settlement.confirm(OccurredAt.of(command.confirmedAt()));
        settlementRepository.save(settlement);
    }

    @Override
    @Transactional
    public void pay(PaySettlementCommand command) {
        Settlement settlement = loadOrThrow(CreatorId.of(command.creatorId()), command.yearMonth());
        settlement.pay(OccurredAt.of(command.paidAt()));
        settlementRepository.save(settlement);
    }

    @Override
    public SettlementRangeView getSettlementsInRange(SettlementRangeQuery query) {
        List<CreatorId> allCreatorIds = creatorRepository.findAllCreatorIds();
        LocalDateTime fromDT = query.from().atStartOfDay();
        LocalDateTime toExclusiveDT = query.to().plusDays(1).atStartOfDay();

        List<SalesRecordView> salesViews = salesRepository.findSalesView(Optional.empty(), fromDT, toExclusiveDT);
        List<CancellationView> cancellationViews = salesRepository.findCancellationsByDateRange(fromDT, toExclusiveDT);

        Map<CreatorId, BigDecimal> totalSalesByCreator = new HashMap<>();
        for (SalesRecordView view : salesViews) {
            totalSalesByCreator.merge(view.creatorId(), view.record().getPaymentAmount().value(), BigDecimal::add);
        }
        Map<CreatorId, BigDecimal> totalRefundByCreator = new HashMap<>();
        for (CancellationView view : cancellationViews) {
            totalRefundByCreator.merge(view.creatorId(), view.record().getRefundAmount().value(), BigDecimal::add);
        }

        BigDecimal feeRate = FeeRate.defaultRate().value();
        List<CreatorPayableView> responses = allCreatorIds.stream()
                .map(creatorId -> {
                    BigDecimal totalSales = totalSalesByCreator.getOrDefault(creatorId, BigDecimal.ZERO);
                    BigDecimal totalRefund = totalRefundByCreator.getOrDefault(creatorId, BigDecimal.ZERO);
                    BigDecimal net = totalSales.subtract(totalRefund);
                    BigDecimal fee = net.signum() < 0
                            ? BigDecimal.ZERO
                            : net.multiply(feeRate).setScale(0, RoundingMode.HALF_UP);
                    BigDecimal expectedSettlementAmount = net.subtract(fee);
                    return new CreatorPayableView(creatorId.value(), expectedSettlementAmount);
                })
                .toList();

        BigDecimal totalAmount = responses.stream()
                .map(CreatorPayableView::expectedSettlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SettlementRangeView(responses, totalAmount);
    }

    private Settlement calculatePending(CreatorId creatorId, YearMonth yearMonth) {
        SalesSummary sa = salesRepository.findSalesSummaryByCreatorAndMonth(creatorId, yearMonth);
        CancellationSummary ca = salesRepository.findCancellationSummaryByCreatorAndMonth(creatorId, yearMonth);
        return monthlySettlementCalculator.calculate(
                creatorId, yearMonth,
                sa.totalAmount(), ca.totalRefund(),
                sa.count(), ca.count(),
                FeeRate.defaultRate()
        );
    }

    private Settlement loadOrThrow(CreatorId creatorId, YearMonth yearMonth) {
        return settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth)
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_NOT_FOUND.message()));
    }
}

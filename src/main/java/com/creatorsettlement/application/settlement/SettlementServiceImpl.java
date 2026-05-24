package com.creatorsettlement.application.settlement;

import com.creatorsettlement.application.fee.FeePolicyService;
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
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.repository.creator.CreatorRepository;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.CancellationView;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.repository.settlement.SettlementRepository;
import com.creatorsettlement.domain.service.settlement.MonthlySettlementCalculator;
import com.creatorsettlement.domain.service.settlement.SettlementAmountCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final SalesRepository salesRepository;
    private final CreatorRepository creatorRepository;
    private final MonthlySettlementCalculator monthlySettlementCalculator;
    private final SettlementAmountCalculator settlementAmountCalculator;
    private final FeePolicyService feePolicyService;

    public SettlementServiceImpl(
            SettlementRepository settlementRepository,
            SalesRepository salesRepository,
            CreatorRepository creatorRepository,
            MonthlySettlementCalculator monthlySettlementCalculator,
            SettlementAmountCalculator settlementAmountCalculator,
            FeePolicyService feePolicyService
    ) {
        this.settlementRepository = settlementRepository;
        this.salesRepository = salesRepository;
        this.creatorRepository = creatorRepository;
        this.monthlySettlementCalculator = monthlySettlementCalculator;
        this.settlementAmountCalculator = settlementAmountCalculator;
        this.feePolicyService = feePolicyService;
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

        List<SalesRecordView> salesViews = salesRepository.findAllSalesView(fromDT, toExclusiveDT);
        List<CancellationView> cancellationViews = salesRepository.findCancellationsByDateRange(fromDT, toExclusiveDT);

        Map<CreatorId, Map<YearMonth, BigDecimal>> salesByCreatorMonth = groupSalesByCreatorMonth(salesViews);
        Map<CreatorId, Map<YearMonth, BigDecimal>> refundByCreatorMonth = groupRefundsByCreatorMonth(cancellationViews);

        Map<YearMonth, FeeRate> rateCache = new HashMap<>();
        List<CreatorPayableView> responses = allCreatorIds.stream()
                .map(creatorId -> {
                    BigDecimal creatorExpected = expectedForCreator(
                            creatorId, salesByCreatorMonth, refundByCreatorMonth, rateCache);
                    return new CreatorPayableView(creatorId.value(), creatorExpected);
                })
                .toList();

        BigDecimal totalAmount = responses.stream()
                .map(CreatorPayableView::expectedSettlementAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SettlementRangeView(responses, totalAmount);
    }

    private Map<CreatorId, Map<YearMonth, BigDecimal>> groupSalesByCreatorMonth(List<SalesRecordView> salesViews) {
        Map<CreatorId, Map<YearMonth, BigDecimal>> result = new HashMap<>();
        for (SalesRecordView view : salesViews) {
            YearMonth ym = YearMonth.from(view.record().getPaidAt().value().toLocalDate());
            result.computeIfAbsent(view.creatorId(), k -> new HashMap<>())
                    .merge(ym, view.record().getPaymentAmount().value(), BigDecimal::add);
        }
        return result;
    }

    private Map<CreatorId, Map<YearMonth, BigDecimal>> groupRefundsByCreatorMonth(List<CancellationView> cancellationViews) {
        Map<CreatorId, Map<YearMonth, BigDecimal>> result = new HashMap<>();
        for (CancellationView view : cancellationViews) {
            YearMonth ym = YearMonth.from(view.record().getCancelledAt().value().toLocalDate());
            result.computeIfAbsent(view.creatorId(), k -> new HashMap<>())
                    .merge(ym, view.record().getRefundAmount().value(), BigDecimal::add);
        }
        return result;
    }

    private BigDecimal expectedForCreator(
            CreatorId creatorId,
            Map<CreatorId, Map<YearMonth, BigDecimal>> salesByCreatorMonth,
            Map<CreatorId, Map<YearMonth, BigDecimal>> refundByCreatorMonth,
            Map<YearMonth, FeeRate> rateCache
    ) {
        Map<YearMonth, BigDecimal> creatorSales = salesByCreatorMonth.getOrDefault(creatorId, Map.of());
        Map<YearMonth, BigDecimal> creatorRefunds = refundByCreatorMonth.getOrDefault(creatorId, Map.of());
        Set<YearMonth> activeMonths = new HashSet<>();
        activeMonths.addAll(creatorSales.keySet());
        activeMonths.addAll(creatorRefunds.keySet());

        BigDecimal creatorExpected = BigDecimal.ZERO;
        for (YearMonth ym : activeMonths) {
            FeeRate monthRate = rateFor(ym, rateCache);
            BigDecimal monthSales = creatorSales.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal monthRefund = creatorRefunds.getOrDefault(ym, BigDecimal.ZERO);
            BigDecimal monthExpected = settlementAmountCalculator.calculateExpectedPayout(
                    Money.of(monthSales), Money.of(monthRefund), monthRate);
            creatorExpected = creatorExpected.add(monthExpected);
        }
        return creatorExpected;
    }

    private FeeRate rateFor(YearMonth ym, Map<YearMonth, FeeRate> rateCache) {
        return rateCache.computeIfAbsent(ym, k -> feePolicyService.findEffectiveRate(k.atDay(1)));
    }

    private Settlement calculatePending(CreatorId creatorId, YearMonth yearMonth) {
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

    private Settlement loadOrThrow(CreatorId creatorId, YearMonth yearMonth) {
        return settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth)
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorMessage.SETTLEMENT_NOT_FOUND.message()));
    }
}

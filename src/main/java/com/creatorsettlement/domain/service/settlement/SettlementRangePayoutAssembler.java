package com.creatorsettlement.domain.service.settlement;

import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.repository.creator.CreatorRepository;
import com.creatorsettlement.domain.repository.sales.dto.MonthlyCancellationAggregate;
import com.creatorsettlement.domain.repository.sales.dto.MonthlySalesAggregate;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class SettlementRangePayoutAssembler {

    private final SalesRepository salesRepository;
    private final CreatorRepository creatorRepository;
    private final CreatorRangePayoutCalculator creatorRangePayoutCalculator;

    public SettlementRangePayoutAssembler(
            SalesRepository salesRepository,
            CreatorRepository creatorRepository,
            CreatorRangePayoutCalculator creatorRangePayoutCalculator
    ) {
        this.salesRepository = salesRepository;
        this.creatorRepository = creatorRepository;
        this.creatorRangePayoutCalculator = creatorRangePayoutCalculator;
    }

    public SettlementRangePayoutResult assemble(LocalDate from, LocalDate to) {
        LocalDateTime fromDate = from.atStartOfDay();
        LocalDateTime toExclusiveDate = to.plusDays(1).atStartOfDay();

        List<MonthlySalesAggregate> salesAggregates =
                salesRepository.findMonthlySalesAggregates(fromDate, toExclusiveDate);
        List<MonthlyCancellationAggregate> cancellationAggregates =
                salesRepository.findMonthlyCancellationAggregates(fromDate, toExclusiveDate);

        Map<CreatorId, Map<YearMonth, BigDecimal>> salesByCreatorMonth = toCreatorMonthMap(
                salesAggregates, MonthlySalesAggregate::creatorId, MonthlySalesAggregate::yearMonth,
                agg -> agg.totalAmount().value());
        Map<CreatorId, Map<YearMonth, BigDecimal>> refundByCreatorMonth = toCreatorMonthMap(
                cancellationAggregates, MonthlyCancellationAggregate::creatorId, MonthlyCancellationAggregate::yearMonth,
                agg -> agg.totalRefund().value());

        List<CreatorId> allCreatorIds = creatorRepository.findAllCreatorIds();
        List<CreatorRangePayout> payouts = allCreatorIds.stream()
                .map(creatorId -> {
                    Map<YearMonth, BigDecimal> creatorSales = salesByCreatorMonth.getOrDefault(creatorId, Map.of());
                    Map<YearMonth, BigDecimal> creatorRefunds = refundByCreatorMonth.getOrDefault(creatorId, Map.of());
                    BigDecimal creatorExpected = creatorRangePayoutCalculator.calculate(
                            creatorSales, creatorRefunds);
                    return new CreatorRangePayout(creatorId, creatorExpected);
                })
                .toList();

        BigDecimal totalAmount = payouts.stream()
                .map(CreatorRangePayout::expectedPayout)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SettlementRangePayoutResult(payouts, totalAmount);
    }

    private static <T> Map<CreatorId, Map<YearMonth, BigDecimal>> toCreatorMonthMap(
            List<T> items,
            Function<T, CreatorId> creatorIdFn,
            Function<T, YearMonth> yearMonthFn,
            Function<T, BigDecimal> amountFn) {
        Map<CreatorId, Map<YearMonth, BigDecimal>> result = new HashMap<>();
        for (T item : items) {
            result.computeIfAbsent(creatorIdFn.apply(item), k -> new HashMap<>())
                    .merge(yearMonthFn.apply(item), amountFn.apply(item), BigDecimal::add);
        }
        return result;
    }
}

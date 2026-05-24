package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.MonthlyCancellationAggregate;
import com.creatorsettlement.domain.repository.sales.dto.MonthlySalesAggregate;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordWithId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemorySalesRepository implements SalesRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<SalesRecordId, SalesRecord> salesById = new ConcurrentHashMap<>();
    private final List<CancellationRecord> cancellations = new CopyOnWriteArrayList<>();
    private final InMemoryCourseRepository courseRepository;

    public InMemorySalesRepository(InMemoryCourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public void saveSalesRecord(SalesRecord salesRecord) {
        SalesRecordId id = SalesRecordId.of(sequence.incrementAndGet());
        salesById.put(id, salesRecord);
    }

    @Override
    public boolean existsById(SalesRecordId salesRecordId) {
        return salesById.containsKey(salesRecordId);
    }

    @Override
    public Optional<SalesRecord> findById(SalesRecordId salesRecordId) {
        return Optional.ofNullable(salesById.get(salesRecordId));
    }

    @Override
    public List<CancellationRecord> findCancellationsBySalesRecordId(SalesRecordId salesRecordId) {
        return cancellations.stream().filter(c -> c.getSalesRecordId().equals(salesRecordId)).toList();
    }

    @Override
    public void saveCancellationRecord(CancellationRecord cancellationRecord) {
        cancellations.add(cancellationRecord);
    }

    private List<SalesRecordWithId> findByPeriod(LocalDateTime from, LocalDateTime toExclusive) {
        return salesById.entrySet().stream()
                .filter(e -> {
                    LocalDateTime paidAt = e.getValue().getPaidAt().value();
                    return !paidAt.isBefore(from) && paidAt.isBefore(toExclusive);
                })
                .map(e -> new SalesRecordWithId(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private List<SalesRecordWithId> findByPeriodAndCourseIds(LocalDateTime from, LocalDateTime toExclusive, Collection<CourseId> courseIds) {
        Set<CourseId> courseIdSet = new HashSet<>(courseIds);
        return salesById.entrySet().stream()
                .filter(e -> {
                    SalesRecord record = e.getValue();
                    LocalDateTime paidAt = record.getPaidAt().value();
                    return courseIdSet.contains(record.getCourseId())
                            && !paidAt.isBefore(from) && paidAt.isBefore(toExclusive);
                })
                .map(e -> new SalesRecordWithId(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private Map<SalesRecordId, List<CancellationRecord>> findCancellationsBySalesRecordIds(Collection<SalesRecordId> ids) {
        Set<SalesRecordId> idSet = new HashSet<>(ids);
        return cancellations.stream()
                .filter(c -> idSet.contains(c.getSalesRecordId()))
                .collect(Collectors.groupingBy(CancellationRecord::getSalesRecordId));
    }

    @Override
    public List<SalesRecordView> findSalesViewPaged(CreatorId creatorId, LocalDateTime from, LocalDateTime toExclusive, int page, int size) {
        List<SalesRecordWithId> filtered;
        if (creatorId == null) {
            filtered = findByPeriod(from, toExclusive);
        } else {
            List<CourseId> courseIds = courseRepository.findCourseIdsByCreatorId(creatorId);
            if (courseIds.isEmpty()) {
                return List.of();
            }
            filtered = findByPeriodAndCourseIds(from, toExclusive, courseIds);
        }
        List<SalesRecordWithId> paged = filtered.stream()
                .sorted(Comparator.comparing((SalesRecordWithId s) -> s.record().getPaidAt().value()).reversed())
                .skip((long) page * size)
                .limit(size)
                .toList();
        return buildViews(paged);
    }

    private Set<CourseId> courseIdSetOf(CreatorId creatorId) {
        return new HashSet<>(courseRepository.findCourseIdsByCreatorId(creatorId));
    }

    private List<SalesRecordView> buildViews(List<SalesRecordWithId> sales) {
        if (sales.isEmpty()) {
            return List.of();
        }
        List<SalesRecordId> saleIds = sales.stream().map(SalesRecordWithId::id).toList();
        Map<SalesRecordId, List<CancellationRecord>> cancellationsBySaleId = findCancellationsBySalesRecordIds(saleIds);

        List<CourseId> courseIdsInSales = sales.stream().map(s -> s.record().getCourseId()).distinct().toList();
        Map<CourseId, CreatorId> creatorIdByCourseId = courseRepository.findCreatorIdsByCourseIds(courseIdsInSales);

        return sales.stream()
                .map(s -> new SalesRecordView(
                        s.id(),
                        s.record(),
                        cancellationsBySaleId.getOrDefault(s.id(), List.of()),
                        creatorIdByCourseId.get(s.record().getCourseId())
                ))
                .toList();
    }

    @Override
    public List<SalesRecordWithId> findByCourseIdAndStudentId(CourseId courseId, StudentId studentId) {
        return salesById.entrySet().stream()
                .filter(e -> e.getValue().getCourseId().equals(courseId) && e.getValue().getStudentId().equals(studentId))
                .map(e -> new SalesRecordWithId(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public SalesSummary findSalesSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth) {
        Set<CourseId> courseIdSet = courseIdSetOf(creatorId);
        if (courseIdSet.isEmpty()) {
            return new SalesSummary(Money.of(BigDecimal.ZERO), 0L);
        }
        List<SalesRecord> filtered = salesById.values().stream()
                .filter(sale -> courseIdSet.contains(sale.getCourseId())
                        && YearMonth.from(sale.getPaidAt().value()).equals(yearMonth))
                .toList();
        BigDecimal total = filtered.stream()
                .map(sale -> sale.getPaymentAmount().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesSummary(Money.of(total), filtered.size());
    }

    @Override
    public CancellationSummary findCancellationSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth) {
        Set<CourseId> courseIdSet = courseIdSetOf(creatorId);
        if (courseIdSet.isEmpty()) {
            return new CancellationSummary(Money.of(BigDecimal.ZERO), 0L);
        }
        List<CancellationRecord> filtered = cancellations.stream()
                .filter(cancellation -> YearMonth.from(cancellation.getCancelledAt().value()).equals(yearMonth))
                .filter(cancellation -> {
                    SalesRecord sale = salesById.get(cancellation.getSalesRecordId());
                    return sale != null && courseIdSet.contains(sale.getCourseId());
                })
                .toList();
        BigDecimal total = filtered.stream()
                .map(cancellation -> cancellation.getRefundAmount().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CancellationSummary(Money.of(total), filtered.size());
    }

    public List<SalesRecord> findAll() {
        return new ArrayList<>(salesById.values());
    }

    @Override
    public List<MonthlySalesAggregate> findMonthlySalesAggregates(LocalDateTime from, LocalDateTime toExclusive) {
        List<SalesRecord> filtered = salesById.values().stream()
                .filter(sale -> {
                    LocalDateTime paidAt = sale.getPaidAt().value();
                    return !paidAt.isBefore(from) && paidAt.isBefore(toExclusive);
                })
                .toList();
        if (filtered.isEmpty()) {
            return List.of();
        }
        List<CourseId> courseIds = filtered.stream().map(SalesRecord::getCourseId).distinct().toList();
        Map<CourseId, CreatorId> creatorIdByCourseId = courseRepository.findCreatorIdsByCourseIds(courseIds);
        Map<AggregateKey, BigDecimal> totals = new java.util.LinkedHashMap<>();
        for (SalesRecord sale : filtered) {
            CreatorId creatorId = creatorIdByCourseId.get(sale.getCourseId());
            if (creatorId == null) {
                continue;
            }
            AggregateKey key = new AggregateKey(creatorId, YearMonth.from(sale.getPaidAt().value()));
            totals.merge(key, sale.getPaymentAmount().value(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(e -> new MonthlySalesAggregate(e.getKey().creatorId(), e.getKey().yearMonth(), Money.of(e.getValue())))
                .toList();
    }

    @Override
    public List<MonthlyCancellationAggregate> findMonthlyCancellationAggregates(LocalDateTime from, LocalDateTime toExclusive) {
        List<CancellationRecord> filtered = cancellations.stream()
                .filter(c -> {
                    LocalDateTime cancelledAt = c.getCancelledAt().value();
                    return !cancelledAt.isBefore(from) && cancelledAt.isBefore(toExclusive);
                })
                .toList();
        if (filtered.isEmpty()) {
            return List.of();
        }
        List<CourseId> courseIds = filtered.stream()
                .map(c -> salesById.get(c.getSalesRecordId()))
                .filter(sale -> sale != null)
                .map(SalesRecord::getCourseId)
                .distinct()
                .toList();
        Map<CourseId, CreatorId> creatorIdByCourseId = courseRepository.findCreatorIdsByCourseIds(courseIds);
        Map<AggregateKey, BigDecimal> totals = new java.util.LinkedHashMap<>();
        for (CancellationRecord cancellation : filtered) {
            SalesRecord sale = salesById.get(cancellation.getSalesRecordId());
            if (sale == null) {
                continue;
            }
            CreatorId creatorId = creatorIdByCourseId.get(sale.getCourseId());
            if (creatorId == null) {
                continue;
            }
            AggregateKey key = new AggregateKey(creatorId, YearMonth.from(cancellation.getCancelledAt().value()));
            totals.merge(key, cancellation.getRefundAmount().value(), BigDecimal::add);
        }
        return totals.entrySet().stream()
                .map(e -> new MonthlyCancellationAggregate(e.getKey().creatorId(), e.getKey().yearMonth(), Money.of(e.getValue())))
                .toList();
    }

    private record AggregateKey(CreatorId creatorId, YearMonth yearMonth) {
    }
}

package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.SalesRecordWithId;
import com.creatorsettlement.domain.repository.SalesRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class InMemorySalesRepository implements SalesRepository {

    private final AtomicLong sequence = new AtomicLong();
    private final Map<SalesRecordId, SalesRecord> salesById = new ConcurrentHashMap<>();
    private final List<CancellationRecord> cancellations = new CopyOnWriteArrayList<>();

    @Override
    public void saveSalesRecord(SalesRecord salesRecord) {
        SalesRecordId id = SalesRecordId.of(sequence.incrementAndGet());
        salesById.put(id, salesRecord);
    }

    @Override
    public Optional<SalesRecord> findById(SalesRecordId salesRecordId) {
        return Optional.ofNullable(salesById.get(salesRecordId));
    }

    @Override
    public Money sumRefundsBySalesRecordId(SalesRecordId salesRecordId) {
        BigDecimal sum = cancellations.stream()
                .filter(c -> c.getSalesRecordId().equals(salesRecordId))
                .map(c -> c.getRefundAmount().value())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Money.of(sum);
    }

    @Override
    public void saveCancellationRecord(CancellationRecord cancellationRecord) {
        cancellations.add(cancellationRecord);
    }

    @Override
    public List<SalesRecordWithId> findByPeriod(LocalDateTime from, LocalDateTime toExclusive) {
        return salesById.entrySet().stream()
                .filter(e -> {
                    LocalDateTime paidAt = e.getValue().getPaidAt().value();
                    return !paidAt.isBefore(from) && paidAt.isBefore(toExclusive);
                })
                .map(e -> new SalesRecordWithId(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SalesRecordWithId> findByPeriodAndCourseIds(LocalDateTime from, LocalDateTime toExclusive, Collection<CourseId> courseIds) {
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

    @Override
    public Map<SalesRecordId, List<CancellationRecord>> findCancellationsBySalesRecordIds(Collection<SalesRecordId> ids) {
        Set<SalesRecordId> idSet = new HashSet<>(ids);
        return cancellations.stream()
                .filter(c -> idSet.contains(c.getSalesRecordId()))
                .collect(Collectors.groupingBy(CancellationRecord::getSalesRecordId));
    }

    public List<SalesRecord> findAll() {
        return new ArrayList<>(salesById.values());
    }
}

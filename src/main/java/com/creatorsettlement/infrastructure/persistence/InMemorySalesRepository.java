package com.creatorsettlement.infrastructure.persistence;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.SalesRecordWithId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;

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
    public List<SalesRecordView> findSalesView(Optional<CreatorId> creatorId, LocalDateTime from, LocalDateTime toExclusive) {
        List<SalesRecordWithId> sales;
        if (creatorId.isEmpty()) {
            sales = findByPeriod(from, toExclusive);
        } else {
            List<CourseId> courseIds = courseRepository.findCourseIdsByCreatorId(creatorId.get());
            if (courseIds.isEmpty()) {
                return List.of();
            }
            sales = findByPeriodAndCourseIds(from, toExclusive, courseIds);
        }
        return buildViews(sales);
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

    public List<SalesRecord> findAll() {
        return new ArrayList<>(salesById.values());
    }
}

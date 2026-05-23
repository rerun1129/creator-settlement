package com.creatorsettlement.infrastructure.persistence.sales;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.CancellationView;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordWithId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaSalesRepository implements SalesRepository {

    private final SalesRecordJpaDataRepository salesDataRepository;
    private final CancellationJpaDataRepository cancellationDataRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaSalesRepository(SalesRecordJpaDataRepository salesDataRepository, CancellationJpaDataRepository cancellationDataRepository) {
        this.salesDataRepository = salesDataRepository;
        this.cancellationDataRepository = cancellationDataRepository;
    }

    @Override
    public void saveSalesRecord(SalesRecord record) {
        CourseJpaEntity courseReference = entityManager.getReference(CourseJpaEntity.class, record.getCourseId().value());
        salesDataRepository.save(SalesRecordMapper.toEntity(record, courseReference));
    }

    @Override
    public boolean existsById(SalesRecordId salesRecordId) {
        return salesDataRepository.existsById(salesRecordId.value());
    }

    @Override
    public Optional<SalesRecord> findById(SalesRecordId salesRecordId) {
        return salesDataRepository.findById(salesRecordId.value()).map(SalesRecordMapper::toDomainSalesRecord);
    }

    @Override
    public List<CancellationRecord> findCancellationsBySalesRecordId(SalesRecordId salesRecordId) {
        return cancellationDataRepository.findBySalesRecordId(salesRecordId.value())
                .stream().map(SalesRecordMapper::toDomainCancellation).toList();
    }

    @Override
    public void saveCancellationRecord(CancellationRecord record) {
        cancellationDataRepository.save(SalesRecordMapper.toCancellationEntity(record));
    }

    @Override
    public List<SalesRecordView> findSalesView(Optional<CreatorId> creatorId, LocalDateTime from, LocalDateTime toExclusive) {
        List<SalesRecordJpaEntity> sales;
        if (creatorId.isEmpty()) {
            sales = salesDataRepository.findByPeriod(from, toExclusive);
        } else {
            sales = salesDataRepository.findByCreatorIdAndPeriod(creatorId.get().value(), from, toExclusive);
        }

        if (sales.isEmpty()) {
            return List.of();
        }

        List<Long> salesIds = sales.stream().map(SalesRecordJpaEntity::getId).toList();
        List<CancellationRecordJpaEntity> cancellations = cancellationDataRepository.findAllBySalesRecordIdIn(salesIds);
        Map<Long, List<CancellationRecordJpaEntity>> cancelByRecordId = cancellations.stream()
                .collect(Collectors.groupingBy(CancellationRecordJpaEntity::getSalesRecordId));

        return SalesRecordViewAssembler.assemble(sales, cancelByRecordId);
    }

    @Override
    public List<CancellationView> findCancellationsByDateRange(LocalDateTime from, LocalDateTime toExclusive) {
        List<CancellationRecordJpaEntity> cancellations = cancellationDataRepository.findByCancelledAtBetween(from, toExclusive);
        if (cancellations.isEmpty()) {
            return List.of();
        }

        List<Long> salesIds = cancellations.stream().map(CancellationRecordJpaEntity::getSalesRecordId).distinct().toList();
        List<SalesRecordJpaEntity> sales = salesDataRepository.findAllByIdIn(salesIds);
        Map<Long, Long> creatorIdBySalesId = sales.stream()
                .collect(Collectors.toMap(SalesRecordJpaEntity::getId, s -> s.getCourse().getCreatorId()));

        return cancellations.stream()
                .map(c -> {
                    Long creatorIdValue = creatorIdBySalesId.get(c.getSalesRecordId());
                    if (creatorIdValue == null) {
                        return null;
                    }
                    return new CancellationView(SalesRecordMapper.toDomainCancellation(c), CreatorId.of(creatorIdValue));
                })
                .filter(view -> view != null)
                .toList();
    }

    @Override
    public List<SalesRecordWithId> findByCourseIdAndStudentId(CourseId courseId, StudentId studentId) {
        return salesDataRepository.findByCourse_IdAndStudentId(courseId.value(), studentId.value())
                .stream()
                .map(entity -> new SalesRecordWithId(
                        SalesRecordId.of(entity.getId()),
                        SalesRecordMapper.toDomainSalesRecord(entity)))
                .toList();
    }

    @Override
    public SalesSummary findSalesSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth) {
        LocalDateTime startInclusive = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        List<SalesRecordJpaEntity> sales = salesDataRepository.findByCreatorIdAndPeriod(creatorId.value(), startInclusive, endExclusive);
        BigDecimal total = sales.stream()
                .map(SalesRecordJpaEntity::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesSummary(Money.of(total), sales.size());
    }

    @Override
    public CancellationSummary findCancellationSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth) {
        LocalDateTime startInclusive = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endExclusive = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        List<CancellationRecordJpaEntity> cancellations = cancellationDataRepository.findByCreatorAndCancelledAtBetween(creatorId.value(), startInclusive, endExclusive);
        BigDecimal total = cancellations.stream()
                .map(CancellationRecordJpaEntity::getRefundAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CancellationSummary(Money.of(total), cancellations.size());
    }
}

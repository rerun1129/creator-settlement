package com.creatorsettlement.infrastructure.persistence.sales;

import com.creatorsettlement.domain.model.sales.CancellationRecord;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.dto.CancellationSummary;
import com.creatorsettlement.domain.repository.sales.dto.SalesSummary;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordWithId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

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
        throw new UnsupportedOperationException("Phase 3 JPA 구현 예정");
    }

    @Override
    public CancellationSummary findCancellationSummaryByCreatorAndMonth(CreatorId creatorId, YearMonth yearMonth) {
        throw new UnsupportedOperationException("Phase 3 JPA 구현 예정");
    }
}

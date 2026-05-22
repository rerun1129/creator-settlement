package com.creatorsettlement.infrastructure.persistence.sale;

import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.SalesRecordView;
import com.creatorsettlement.domain.repository.SalesRepository;
import com.creatorsettlement.infrastructure.persistence.course.CourseJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
    public Optional<SalesRecord> findById(SalesRecordId salesRecordId) {
        return salesDataRepository.findById(salesRecordId.value()).map(SalesRecordMapper::toDomainSalesRecord);
    }

    @Override
    public Money sumRefundsBySalesRecordId(SalesRecordId salesRecordId) {
        return Money.of(cancellationDataRepository.sumRefundAmountBySalesRecordId(salesRecordId.value()));
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
}

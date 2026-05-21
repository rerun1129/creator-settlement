package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.CourseRepository;
import com.creatorsettlement.domain.repository.SalesRecordWithId;
import com.creatorsettlement.domain.repository.SalesRepository;
import com.creatorsettlement.domain.service.RefundPolicy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;
    private final CourseRepository courseRepository;
    private final RefundPolicy refundPolicy;

    public SalesServiceImpl(SalesRepository salesRepository, CourseRepository courseRepository, RefundPolicy refundPolicy) {
        this.salesRepository = salesRepository;
        this.courseRepository = courseRepository;
        this.refundPolicy = refundPolicy;
    }

    @Override
    public void register(RegisterSaleCommand command) {
        salesRepository.saveSalesRecord(command.toSalesRecord());
    }

    @Override
    public void registerCancellation(RegisterCancellationCommand command) {
        SalesRecordId salesRecordId = SalesRecordId.of(command.salesRecordId());
        SalesRecord sale = salesRepository.findById(salesRecordId)
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorMessage.SALES_RECORD_NOT_FOUND.message()));
        refundPolicy.enforceRefundLimit(sale, salesRecordId, Money.of(command.refundAmount()));
        salesRepository.saveCancellationRecord(command.toCancellationRecord());
    }

    @Override
    public List<SalesListItem> listSales(ListSalesQuery query) {
        List<SalesRecordWithId> sales = fetchSales(query);
        if (sales.isEmpty()) {
            return List.of();
        }

        List<SalesRecordId> saleIds = sales.stream().map(SalesRecordWithId::id).toList();
        Map<SalesRecordId, List<CancellationRecord>> cancellationsBySaleId = salesRepository.findCancellationsBySalesRecordIds(saleIds);

        List<CourseId> courseIdsInSales = sales.stream().map(s -> s.record().getCourseId()).distinct().toList();
        Map<CourseId, CreatorId> creatorIdByCourseId = courseRepository.findCreatorIdsByCourseIds(courseIdsInSales);

        return sales.stream()
                .map(s -> toSalesListItem(
                        s,
                        cancellationsBySaleId.getOrDefault(s.id(), List.of()),
                        creatorIdByCourseId.get(s.record().getCourseId())
                ))
                .toList();
    }

    private List<SalesRecordWithId> fetchSales(ListSalesQuery query) {
        if (query.creatorId() == null) {
            return salesRepository.findByPeriod(query.from(), query.toExclusive());
        }
        CreatorId creatorId = CreatorId.of(query.creatorId());
        List<CourseId> courseIds = courseRepository.findCourseIdsByCreatorId(creatorId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return salesRepository.findByPeriodAndCourseIds(query.from(), query.toExclusive(), courseIds);
    }

    private SalesListItem toSalesListItem(SalesRecordWithId entry, List<CancellationRecord> cancellations, CreatorId creatorId) {
        List<CancellationView> views = cancellations.stream()
                .map(c -> new CancellationView(c.getRefundAmount(), c.getCancelledAt()))
                .toList();
        return new SalesListItem(
                entry.id(),
                entry.record().getCourseId(),
                entry.record().getStudentId(),
                creatorId,
                entry.record().getPaymentAmount(),
                entry.record().getPaidAt(),
                views
        );
    }
}

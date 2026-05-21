package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.CourseRepository;
import com.creatorsettlement.domain.repository.SalesRecordView;
import com.creatorsettlement.domain.repository.SalesRepository;
import com.creatorsettlement.domain.service.RefundPolicy;
import org.springframework.stereotype.Service;

import java.util.List;

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
        CourseId courseId = CourseId.of(command.courseId());
        if (!courseRepository.existsByCourseId(courseId)) {
            throw new IllegalArgumentException(DomainErrorMessage.COURSE_NOT_FOUND_FOR_REGISTRATION.message());
        }
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
        return fetchSalesViews(query).stream()
                .map(this::toSalesListItem)
                .toList();
    }

    private List<SalesRecordView> fetchSalesViews(ListSalesQuery query) {
        if (query.creatorId() == null) {
            return salesRepository.findSalesViewByPeriod(query.from(), query.toExclusive());
        }
        CreatorId creatorId = CreatorId.of(query.creatorId());
        List<CourseId> courseIds = courseRepository.findCourseIdsByCreatorId(creatorId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return salesRepository.findSalesViewByPeriodAndCourseIds(query.from(), query.toExclusive(), courseIds);
    }

    private SalesListItem toSalesListItem(SalesRecordView view) {
        List<CancellationView> cancellationViews = view.cancellations().stream()
                .map(c -> new CancellationView(c.getRefundAmount(), c.getCancelledAt()))
                .toList();
        return new SalesListItem(
                view.id(),
                view.record().getCourseId(),
                view.record().getStudentId(),
                view.creatorId(),
                view.record().getPaymentAmount(),
                view.record().getPaidAt(),
                cancellationViews
        );
    }
}

package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.CourseRepository;
import com.creatorsettlement.domain.repository.SalesRepository;
import com.creatorsettlement.domain.service.RefundPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    @Override
    public void register(RegisterSaleCommand command) {
        CourseId courseId = CourseId.of(command.courseId());
        if (!courseRepository.existsByCourseId(courseId)) {
            throw new IllegalArgumentException(DomainErrorMessage.COURSE_NOT_FOUND_FOR_REGISTRATION.message());
        }
        salesRepository.saveSalesRecord(command.toSalesRecord());
    }

    @Transactional
    @Override
    public void registerCancellation(RegisterCancellationCommand command) {
        SalesRecordId salesRecordId = SalesRecordId.of(command.salesRecordId());
        SalesRecord sale = salesRepository.findById(salesRecordId)
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorMessage.SALES_RECORD_NOT_FOUND.message()));
        refundPolicy.enforceRefundLimit(sale, salesRecordId, Money.of(command.refundAmount()));
        salesRepository.saveCancellationRecord(command.toCancellationRecord());
    }

    @Transactional(readOnly = true)
    @Override
    public List<SalesListItem> listSales(ListSalesQuery query) {
        return salesRepository.findSalesView(query.toCreatorId(), query.from(), query.toExclusive())
                .stream()
                .map(SalesListItem::from)
                .toList();
    }
}

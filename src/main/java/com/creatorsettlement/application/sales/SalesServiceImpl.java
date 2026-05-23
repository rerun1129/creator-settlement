package com.creatorsettlement.application.sales;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.service.sales.RefundPolicy;
import com.creatorsettlement.domain.service.sales.SaleRegistrationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;
    private final RefundPolicy refundPolicy;
    private final SaleRegistrationPolicy saleRegistrationPolicy;

    public SalesServiceImpl(SalesRepository salesRepository, RefundPolicy refundPolicy, SaleRegistrationPolicy saleRegistrationPolicy) {
        this.salesRepository = salesRepository;
        this.refundPolicy = refundPolicy;
        this.saleRegistrationPolicy = saleRegistrationPolicy;
    }

    @Transactional
    @Override
    public void register(RegisterSaleCommand command) {
        CourseId courseId = CourseId.of(command.courseId());
        StudentId studentId = StudentId.of(command.studentId());
        saleRegistrationPolicy.validateRegistrable(courseId, studentId);
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

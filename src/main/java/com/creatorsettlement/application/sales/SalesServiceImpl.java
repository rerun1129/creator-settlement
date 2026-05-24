package com.creatorsettlement.application.sales;

import com.creatorsettlement.application.sales.dto.ListSalesQuery;
import com.creatorsettlement.application.sales.dto.RegisterCancellationCommand;
import com.creatorsettlement.application.sales.dto.RegisterSaleCommand;
import com.creatorsettlement.application.sales.dto.SalesListItem;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.CreatorId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;
import com.creatorsettlement.domain.repository.sales.dto.SalesRecordView;
import com.creatorsettlement.domain.service.sales.CancellationRegistrationPolicy;
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
    private final CancellationRegistrationPolicy cancellationRegistrationPolicy;

    public SalesServiceImpl(SalesRepository salesRepository, RefundPolicy refundPolicy, SaleRegistrationPolicy saleRegistrationPolicy, CancellationRegistrationPolicy cancellationRegistrationPolicy) {
        this.salesRepository = salesRepository;
        this.refundPolicy = refundPolicy;
        this.saleRegistrationPolicy = saleRegistrationPolicy;
        this.cancellationRegistrationPolicy = cancellationRegistrationPolicy;
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
        cancellationRegistrationPolicy.validate(salesRecordId);
        SalesRecord sale = salesRepository.findById(salesRecordId).orElseThrow(IllegalStateException::new);
        refundPolicy.enforceRefundLimit(sale, salesRecordId, Money.of(command.refundAmount()));
        salesRepository.saveCancellationRecord(command.toCancellationRecord());
    }

    @Transactional(readOnly = true)
    @Override
    public List<SalesListItem> listSales(ListSalesQuery query) {
        CreatorId creatorId = query.toCreatorId().orElse(null);
        List<SalesRecordView> views = salesRepository.findSalesViewPaged(
                creatorId, query.from(), query.toExclusive(), query.page(), query.size());
        return views.stream()
                .map(SalesListItem::from)
                .toList();
    }
}

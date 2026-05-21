package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sale.CancellationRecord;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.CourseId;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.OccurredAt;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.model.vo.StudentId;
import com.creatorsettlement.domain.repository.SalesRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;

    public SalesServiceImpl(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    @Override
    public void register(RegisterSaleCommand command) {
        salesRepository.saveSalesRecord(toSalesRecord(command));
    }

    @Override
    public void registerCancellation(RegisterCancellationCommand command) {
        SalesRecordId salesRecordId = SalesRecordId.of(command.salesRecordId());
        SalesRecord sale = salesRepository.findById(salesRecordId)
                .orElseThrow(() -> new IllegalArgumentException(DomainErrorMessage.SALES_RECORD_NOT_FOUND.message()));
        Money refundAmount = Money.of(command.refundAmount());
        Money cumulative = salesRepository.sumRefundsBySalesRecordId(salesRecordId);
        BigDecimal remain = sale.getPaymentAmount().value().subtract(cumulative.value());
        if (refundAmount.value().compareTo(remain) > 0) {
            throw new IllegalArgumentException(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
        }
        CancellationRecord cancellation = CancellationRecord.of(salesRecordId, refundAmount, OccurredAt.of(command.cancelledAt()));
        salesRepository.saveCancellationRecord(cancellation);
    }

    private SalesRecord toSalesRecord(RegisterSaleCommand command) {
        return SalesRecord.of(
            CourseId.of(command.courseId()),
            StudentId.of(command.studentId()),
            Money.of(command.paymentAmount()),
            OccurredAt.of(command.paidAt())
        );
    }
}

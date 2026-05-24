package com.creatorsettlement.domain.service.sales;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sales.Cancellations;
import com.creatorsettlement.domain.model.sales.SalesRecord;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;

public class RefundPolicy {

    private final SalesRepository salesRepository;

    public RefundPolicy(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public void enforceRefundLimit(SalesRecord sale, SalesRecordId salesRecordId, Money refundAmount) {
        if (refundAmount.value().signum() <= 0) {
            throw new IllegalArgumentException(DomainErrorMessage.REFUND_AMOUNT_NOT_POSITIVE.message());
        }
        Cancellations cancellations = Cancellations.of(salesRepository.findCancellationsBySalesRecordId(salesRecordId));
        Money remain = cancellations.remainingOf(sale.getPaymentAmount());
        if (refundAmount.value().compareTo(remain.value()) > 0) {
            throw new IllegalArgumentException(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
        }
    }
}

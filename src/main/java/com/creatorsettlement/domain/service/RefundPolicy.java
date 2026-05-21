package com.creatorsettlement.domain.service;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.SalesRepository;

import java.math.BigDecimal;

public class RefundPolicy {

    private final SalesRepository salesRepository;

    public RefundPolicy(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public void enforceRefundLimit(SalesRecord sale, SalesRecordId salesRecordId, Money refundAmount) {
        Money cumulative = salesRepository.sumRefundsBySalesRecordId(salesRecordId);
        BigDecimal remain = sale.getPaymentAmount().value().subtract(cumulative.value());
        if (refundAmount.value().compareTo(remain) > 0) {
            throw new IllegalArgumentException(DomainErrorMessage.REFUND_EXCEEDS_REMAINING.message());
        }
    }
}

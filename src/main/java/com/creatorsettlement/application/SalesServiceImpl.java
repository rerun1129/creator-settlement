package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.sale.SalesRecord;
import com.creatorsettlement.domain.model.vo.Money;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.SalesRepository;
import com.creatorsettlement.domain.service.RefundPolicy;
import org.springframework.stereotype.Service;

@Service
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;
    private final RefundPolicy refundPolicy;

    public SalesServiceImpl(SalesRepository salesRepository, RefundPolicy refundPolicy) {
        this.salesRepository = salesRepository;
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
}

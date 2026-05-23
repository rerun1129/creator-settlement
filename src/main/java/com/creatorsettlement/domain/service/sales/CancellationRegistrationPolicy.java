package com.creatorsettlement.domain.service.sales;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.SalesRecordId;
import com.creatorsettlement.domain.repository.sales.SalesRepository;

public class CancellationRegistrationPolicy {

    private final SalesRepository salesRepository;

    public CancellationRegistrationPolicy(SalesRepository salesRepository) {
        this.salesRepository = salesRepository;
    }

    public void validate(SalesRecordId id) {
        if (!salesRepository.existsById(id)) {
            throw new IllegalArgumentException(DomainErrorMessage.SALES_RECORD_NOT_FOUND.message());
        }
    }
}

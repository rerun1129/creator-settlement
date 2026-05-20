package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

public record SalesRecordId(Long value) {

    public SalesRecordId {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.SALES_RECORD_ID_NULL.message());
        }
    }

    public static SalesRecordId of(Long value) {
        return new SalesRecordId(value);
    }
}

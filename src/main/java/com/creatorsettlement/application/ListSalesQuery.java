package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import java.time.LocalDateTime;

public record ListSalesQuery(Long creatorId, LocalDateTime from, LocalDateTime toExclusive) {
    public ListSalesQuery {
        if (from == null) {
            throw new IllegalArgumentException(DomainErrorMessage.LIST_SALES_FROM_NULL.message());
        }
        if (toExclusive == null) {
            throw new IllegalArgumentException(DomainErrorMessage.LIST_SALES_TO_EXCLUSIVE_NULL.message());
        }
        if (from.isAfter(toExclusive)) {
            throw new IllegalArgumentException(DomainErrorMessage.LIST_SALES_PERIOD_INVALID.message());
        }
    }
}

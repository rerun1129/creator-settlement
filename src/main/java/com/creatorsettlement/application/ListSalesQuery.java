package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CreatorId;
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

    public CreatorId toCreatorId() {
        return creatorId == null ? null : CreatorId.of(creatorId);
    }
}

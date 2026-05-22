package com.creatorsettlement.application;

import com.creatorsettlement.domain.error.DomainErrorMessage;
import com.creatorsettlement.domain.model.vo.CreatorId;
import java.time.LocalDateTime;
import java.util.Optional;

public record ListSalesQuery(Long creatorId, LocalDateTime from, LocalDateTime toExclusive) {
    public ListSalesQuery {
        if (from.isAfter(toExclusive)) {
            throw new IllegalArgumentException(DomainErrorMessage.LIST_SALES_PERIOD_INVALID.message());
        }
    }

    public Optional<CreatorId> toCreatorId() {
        return Optional.ofNullable(creatorId).map(CreatorId::of);
    }
}

package com.creatorsettlement.presentation.dto;

import com.creatorsettlement.application.ListSalesQuery;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record ListSalesRequest(
        Long creatorId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toExclusive
) {
    public ListSalesQuery toQuery() {
        return new ListSalesQuery(creatorId, from, toExclusive);
    }
}

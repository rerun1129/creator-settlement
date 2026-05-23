package com.creatorsettlement.presentation.sales.dto;

import com.creatorsettlement.application.sales.dto.ListSalesQuery;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record ListSalesRequest(
        Long creatorId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toExclusive
) {
    public ListSalesQuery toQuery() {
        return new ListSalesQuery(creatorId, from, toExclusive);
    }
}

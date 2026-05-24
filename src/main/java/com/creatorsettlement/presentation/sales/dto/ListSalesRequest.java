package com.creatorsettlement.presentation.sales.dto;

import com.creatorsettlement.application.sales.dto.ListSalesQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record ListSalesRequest(
        Long creatorId,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toExclusive,
        @Min(0) Integer page,
        @Min(1) @Max(1000) Integer size
) {
    public ListSalesQuery toQuery() {
        return ListSalesQuery.of(
                creatorId,
                from,
                toExclusive,
                page == null ? 0 : page,
                size == null ? 1000 : size
        );
    }
}

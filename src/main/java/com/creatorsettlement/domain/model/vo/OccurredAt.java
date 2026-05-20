package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

import java.time.LocalDateTime;

public record OccurredAt(LocalDateTime value) {

    public OccurredAt {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.OCCURRED_AT_NULL.message());
        }
        if (value.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(DomainErrorMessage.OCCURRED_AT_FUTURE.message());
        }
    }
}

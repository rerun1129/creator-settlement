package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

import java.time.LocalDateTime;

public record OccurredAt(LocalDateTime value) {

    public OccurredAt {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.OCCURRED_AT_NULL.message());
        }
    }

    public static OccurredAt of(LocalDateTime value) {
        return new OccurredAt(value);
    }
}

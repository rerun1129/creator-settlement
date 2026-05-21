package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

public record CreatorId(Long value) {

    public CreatorId {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.CREATOR_ID_NULL.message());
        }
    }

    public static CreatorId of(Long value) {
        return new CreatorId(value);
    }
}

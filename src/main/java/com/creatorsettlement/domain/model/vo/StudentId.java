package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

public record StudentId(Long value) {

    public StudentId {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.STUDENT_ID_NULL.message());
        }
    }
}

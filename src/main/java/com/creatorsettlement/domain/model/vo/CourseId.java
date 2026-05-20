package com.creatorsettlement.domain.model.vo;

import com.creatorsettlement.domain.error.DomainErrorMessage;

public record CourseId(Long value) {

    public CourseId {
        if (value == null) {
            throw new IllegalArgumentException(DomainErrorMessage.COURSE_ID_NULL.message());
        }
    }

    public static CourseId of(Long value) {
        return new CourseId(value);
    }
}

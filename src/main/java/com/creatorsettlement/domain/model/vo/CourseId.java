package com.creatorsettlement.domain.model.vo;

public record CourseId(Long value) {

    public CourseId {
        if (value == null) {
            throw new IllegalArgumentException("Course ID는 null일 수 없습니다");
        }
    }
}

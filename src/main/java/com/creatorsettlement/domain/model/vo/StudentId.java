package com.creatorsettlement.domain.model.vo;

public record StudentId(Long value) {

    public StudentId {
        if (value == null) {
            throw new IllegalArgumentException("Student ID는 null일 수 없습니다");
        }
    }
}

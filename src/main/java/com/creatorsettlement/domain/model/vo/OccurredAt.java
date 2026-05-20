package com.creatorsettlement.domain.model.vo;

import java.time.LocalDateTime;

public record OccurredAt(LocalDateTime value) {

    public OccurredAt {
        if (value == null) {
            throw new IllegalArgumentException("발생 일시는 null일 수 없습니다");
        }
        if (value.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("발생 일시는 미래일 수 없습니다");
        }
    }
}

package com.creatorsettlement.domain.model.vo;

public record SalesRecordId(Long value) {

    public SalesRecordId {
        if (value == null) {
            throw new IllegalArgumentException("원본 판매 내역 ID는 null일 수 없습니다");
        }
    }
}

package com.creatorsettlement.domain.error;

public enum DomainErrorMessage {
    MONEY_NULL("금액은 null일 수 없습니다"),
    MONEY_NOT_POSITIVE("금액은 0보다 커야 합니다"),
    OCCURRED_AT_NULL("발생 일시는 null일 수 없습니다"),
    OCCURRED_AT_FUTURE("발생 일시는 미래일 수 없습니다"),
    COURSE_ID_NULL("Course ID는 null일 수 없습니다"),
    STUDENT_ID_NULL("Student ID는 null일 수 없습니다"),
    SALES_RECORD_ID_NULL("원본 판매 내역 ID는 null일 수 없습니다"),
    REFUND_EXCEEDS_REMAINING("환불 금액은 잔여 환불 가능 금액을 초과할 수 없습니다");

    private final String message;

    DomainErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}

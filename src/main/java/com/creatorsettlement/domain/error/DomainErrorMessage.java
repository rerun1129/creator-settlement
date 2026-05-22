package com.creatorsettlement.domain.error;

public enum DomainErrorMessage {
    MONEY_NULL("금액은 null일 수 없습니다"),
    MONEY_NEGATIVE("금액은 음수일 수 없습니다"),
    OCCURRED_AT_NULL("발생 일시는 null일 수 없습니다"),
    COURSE_ID_NULL("Course ID는 null일 수 없습니다"),
    STUDENT_ID_NULL("Student ID는 null일 수 없습니다"),
    CREATOR_ID_NULL("크리에이터 ID는 null일 수 없습니다"),
    SALES_RECORD_ID_NULL("원본 판매 내역 ID는 null일 수 없습니다"),
    REFUND_EXCEEDS_REMAINING("환불 금액은 잔여 환불 가능 금액을 초과할 수 없습니다"),
    SALES_RECORD_NOT_FOUND("원본 판매 내역을 찾을 수 없습니다"),
    LIST_SALES_FROM_NULL("조회 기간 시작값은 null일 수 없습니다"),
    LIST_SALES_TO_EXCLUSIVE_NULL("조회 기간 종료값은 null일 수 없습니다"),
    LIST_SALES_PERIOD_INVALID("조회 기간 시작값은 종료값보다 이후일 수 없습니다"),
    COURSE_NOT_FOUND_FOR_REGISTRATION("등록 대상 강의를 찾을 수 없습니다");

    private final String message;

    DomainErrorMessage(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}

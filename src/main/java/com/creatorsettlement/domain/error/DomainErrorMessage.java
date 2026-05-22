package com.creatorsettlement.domain.error;

public enum DomainErrorMessage {
    MONEY_NULL("금액이 누락되었습니다", 400),
    MONEY_NEGATIVE("금액은 0원 이상이어야 합니다", 400),
    OCCURRED_AT_NULL("발생 일시가 누락되었습니다", 400),
    COURSE_ID_NULL("강의 정보가 누락되었습니다", 400),
    STUDENT_ID_NULL("학생 정보가 누락되었습니다", 400),
    CREATOR_ID_NULL("크리에이터 정보가 누락되었습니다", 400),
    SALES_RECORD_ID_NULL("원본 판매 내역 정보가 누락되었습니다", 400),
    REFUND_EXCEEDS_REMAINING("환불 금액은 잔여 환불 가능 금액을 초과할 수 없습니다", 400),
    SALES_RECORD_NOT_FOUND("원본 판매 내역을 찾을 수 없습니다", 400),
    LIST_SALES_FROM_NULL("조회 기간 시작 일시를 입력해 주세요", 400),
    LIST_SALES_TO_EXCLUSIVE_NULL("조회 기간 종료 일시를 입력해 주세요", 400),
    LIST_SALES_PERIOD_INVALID("조회 시작 일시가 종료 일시보다 늦을 수 없습니다", 400),
    COURSE_NOT_FOUND_FOR_REGISTRATION("등록 대상 강의를 찾을 수 없습니다", 400);

    private final String message;
    private final int httpStatus;

    DomainErrorMessage(String message, int httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String message() {
        return message;
    }

    public int httpStatus() {
        return httpStatus;
    }
}

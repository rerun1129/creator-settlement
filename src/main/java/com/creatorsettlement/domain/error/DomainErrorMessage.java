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
    LIST_SALES_PERIOD_INVALID("조회 시작 일시가 종료 일시보다 늦을 수 없습니다", 400),
    COURSE_NOT_FOUND_FOR_REGISTRATION("등록 대상 강의를 찾을 수 없습니다", 400),
    DUPLICATE_ACTIVE_PURCHASE("동일 학생의 동일 강의에 활성 결제가 이미 존재합니다", 409),
    FEE_RATE_NULL("수수료율 정보가 누락되었습니다", 400),
    FEE_RATE_NEGATIVE("수수료율은 0% 이상이어야 합니다", 400),
    FEE_RATE_GREATER_THAN_ONE("수수료율은 100%를 초과할 수 없습니다", 400),
    SETTLEMENT_AMOUNT_NULL("정산 금액이 누락되었습니다", 400),
    SETTLEMENT_NOT_FOUND("정산 내역을 찾을 수 없습니다", 404),
    SETTLEMENT_ALREADY_CONFIRMED("이미 확정된 정산입니다", 409),
    SETTLEMENT_ALREADY_PAID("이미 지급된 정산입니다", 409),
    SETTLEMENT_NOT_CONFIRMED_FOR_PAYMENT("확정되지 않은 정산은 지급할 수 없습니다", 409),
    FEE_POLICY_EFFECTIVE_FROM_NULL("수수료 정책 적용 시작일이 누락되었습니다", 400),
    FEE_POLICY_DUPLICATE_EFFECTIVE_FROM("동일 적용 시작일의 수수료 정책이 이미 존재합니다", 400);

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

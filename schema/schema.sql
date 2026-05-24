-- ============================================================
-- Creator Settlement Platform - DB Schema (Final)
-- DBMS    : MySQL 8.x
-- Charset : utf8mb4 / Collation: utf8mb4_unicode_ci
-- Engine  : InnoDB
-- ============================================================

CREATE TABLE creator (
    creator_id BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='크리에이터';

CREATE TABLE course (
    course_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_course_creator FOREIGN KEY (creator_id) REFERENCES creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='강의';
CREATE INDEX idx_course_creator ON course (creator_id);

CREATE TABLE sales_record (
    sales_record_id BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id       BIGINT        NOT NULL,
    student_id      BIGINT        NOT NULL,
    payment_amount  DECIMAL(19,2) NOT NULL,
    paid_at         DATETIME(6)   NOT NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_sales_course FOREIGN KEY (course_id) REFERENCES course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='결제 이력 (수강생 → 강의)';
CREATE INDEX idx_sales_paid_at ON sales_record (paid_at);
CREATE INDEX idx_sales_course  ON sales_record (course_id);

CREATE TABLE cancellation_record (
    cancellation_record_id BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sales_record_id        BIGINT        NOT NULL,
    refund_amount          DECIMAL(19,2) NOT NULL,
    cancelled_at           DATETIME(6)   NOT NULL,
    created_at             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cancel_sales FOREIGN KEY (sales_record_id) REFERENCES sales_record (sales_record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='환불 이력 (결제 → 환불)';
CREATE INDEX idx_cancel_sales ON cancellation_record (sales_record_id);

CREATE TABLE settlement (
    settlement_id      BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creator_id         BIGINT        NOT NULL,
    year_month         VARCHAR(6)    NOT NULL                         COMMENT 'YYYYMM',
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING'       COMMENT 'PENDING | CONFIRMED | PAID',
    total_sales        DECIMAL(19,2) NOT NULL,
    total_refund       DECIMAL(19,2) NOT NULL,
    net_sales          DECIMAL(19,2) NOT NULL,
    fee_rate           DECIMAL(5,4)  NOT NULL,
    platform_fee       DECIMAL(19,2) NOT NULL,
    expected_payout    DECIMAL(19,2) NOT NULL,
    sales_count        BIGINT        NOT NULL,
    cancellation_count BIGINT        NOT NULL,
    confirmed_at       DATETIME(6)   NULL,
    paid_at            DATETIME(6)   NULL,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_settlement_creator_yearmonth UNIQUE (creator_id, year_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='월별 정산 스냅샷 (creator 라이프사이클과 분리된 감사 로그 성격으로 FK 미적용)';

CREATE TABLE fee_policy (
    fee_policy_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rate           DECIMAL(5,4) NOT NULL,
    effective_from DATE         NOT NULL,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_fee_policy_effective_from UNIQUE (effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='수수료율 이력 (effective_from 기준 적용, 정책 이력성으로 FK 미적용)';

INSERT INTO fee_policy (rate, effective_from)
VALUES (0.2000, '2020-01-01');

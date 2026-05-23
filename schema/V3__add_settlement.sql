CREATE TABLE IF NOT EXISTS settlement (
    settlement_id      BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    creator_id         BIGINT        NOT NULL,
    year_month         VARCHAR(6)    NOT NULL,
    status             VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    total_sales        DECIMAL(19,2) NOT NULL,
    total_refund       DECIMAL(19,2) NOT NULL,
    net_sales          DECIMAL(19,2) NOT NULL,
    fee_rate           DECIMAL(5,4)  NOT NULL,
    platform_fee       DECIMAL(19,2) NOT NULL,
    expected_payout    DECIMAL(19,2) NOT NULL,
    sales_count        BIGINT        NOT NULL,
    cancellation_count BIGINT        NOT NULL,
    confirmed_at       DATETIME(6)   NULL,
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_settlement_creator_yearmonth UNIQUE (creator_id, year_month)
);

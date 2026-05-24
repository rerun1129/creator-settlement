CREATE TABLE IF NOT EXISTS fee_policy (
    fee_policy_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rate           DECIMAL(5,4) NOT NULL,
    effective_from DATE         NOT NULL,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_fee_policy_effective_from UNIQUE (effective_from)
);

INSERT INTO fee_policy (rate, effective_from)
VALUES (0.2000, '2020-01-01');
